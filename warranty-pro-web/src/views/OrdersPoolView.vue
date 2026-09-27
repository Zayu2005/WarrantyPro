<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { acceptOrder, fetchDispatchRecords, fetchWorkers, reassignOrder } from '@/api/dispatch'
import type { DispatchRecord, WorkerInfo } from '@/api/dispatch'
import { categoryLabel, statusLabel, statusTagType } from '@/utils/labels'

// 工单池（docs/07 §2.3）：受理即智能直派；已派单可查看派单记录 / 人工改派
interface OrderRow {
  id: number
  orderNo: string
  category: string
  locationDetail: string
  phenomenon: string
  urgency: string
  status: string
  verdict: string
  responsibleParty: string
  currentWorkerId: number | null
  submittedAt: string
  createdAt: string
}

const query = reactive({ status: '', category: '', page: 1, pageSize: 10 })
const page = reactive({ list: [] as OrderRow[], total: 0 })
const loading = ref(false)

const statusOptions = [
  { value: 'SUBMITTED', label: '待受理' },
  { value: 'PENDING_DISPATCH', label: '待派单' },
  { value: 'DISPATCHED', label: '已派单·待上门' },
  { value: 'IN_PROGRESS', label: '维修中' },
  { value: 'PENDING_CONFIRM', label: '待验收' },
  { value: 'COMPLETED', label: '已完结' },
]
const categoryOptions = ['WATER_ELECTRICITY', 'CIVIL_WATERPROOF', 'DOOR_WINDOW', 'HVAC', 'ELEVATOR', 'OTHER']

async function load() {
  loading.value = true
  try {
    const data = (await request.get('/dispatch/orders', { params: query })) as unknown as {
      list: OrderRow[]
      total: number
    }
    page.list = data.list
    page.total = data.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function onAccept(row: OrderRow) {
  try {
    await acceptOrder(row.id)
    ElMessage.success(`工单 ${row.orderNo} 已受理并智能直派`)
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '受理失败')
  }
}

// ---- 派单记录弹窗 ----
const recordVisible = ref(false)
const records = ref<DispatchRecord[]>([])
const recordOrder = ref<OrderRow | null>(null)

async function showRecords(row: OrderRow) {
  recordOrder.value = row
  try {
    records.value = await fetchDispatchRecords(row.id)
    recordVisible.value = true
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载派单记录失败')
  }
}

// ---- 人工改派弹窗 ----
const reassignVisible = ref(false)
const reassignOrderRow = ref<OrderRow | null>(null)
const reassignForm = reactive({ workerId: null as number | null, reason: '' })
const workers = ref<WorkerInfo[]>([])
const smartReassign = ref(false)

async function showReassign(row: OrderRow) {
  reassignOrderRow.value = row
  reassignForm.workerId = null
  reassignForm.reason = ''
  smartReassign.value = false
  try {
    workers.value = (await fetchWorkers()).filter((w) => w.todayDuty)
    reassignVisible.value = true
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载在班师傅失败')
  }
}

async function submitReassign() {
  if (!reassignOrderRow.value) return
  if (!smartReassign.value && reassignForm.workerId === null) {
    ElMessage.warning('请选择接手的师傅，或勾选智能重派')
    return
  }
  try {
    await reassignOrder(reassignOrderRow.value.id, smartReassign.value ? null : reassignForm.workerId, reassignForm.reason)
    ElMessage.success('改派完成')
    reassignVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '改派失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="wp-card">
    <div class="head">
      <h3 class="card-title">工单池</h3>
      <div class="filters">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px" @change="load">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="query.category" placeholder="全部类别" clearable style="width: 140px" @change="load">
          <el-option v-for="c in categoryOptions" :key="c" :label="categoryLabel(c)" :value="c" />
        </el-select>
      </div>
    </div>

    <el-table :data="page.list" v-loading="loading">
      <el-table-column prop="orderNo" label="工单号" width="150" />
      <el-table-column label="类别" width="90">
        <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
      </el-table-column>
      <el-table-column label="位置 / 现象" min-width="220">
        <template #default="{ row }">
          {{ row.locationDetail }} · {{ row.phenomenon }}
        </template>
      </el-table-column>
      <el-table-column label="紧急" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.urgency === 'URGENT'" type="danger" size="small">紧急</el-tag>
          <span v-else class="muted">普通</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submittedAt" label="提交时间" width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'SUBMITTED'" type="primary" size="small" @click="onAccept(row)">
            受理并直派
          </el-button>
          <el-button size="small" @click="showRecords(row)">派单记录</el-button>
          <el-button v-if="row.status === 'DISPATCHED'" size="small" @click="showReassign(row)">改派</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="prev, pager, next, total"
      :total="page.total"
      :page-size="query.pageSize"
      :current-page="query.page"
      @current-change="(p: number) => { query.page = p; load() }"
    />

    <!-- 派单记录 -->
    <el-dialog v-model="recordVisible" :title="`派单记录 · ${recordOrder?.orderNo ?? ''}`" width="640px">
      <el-table :data="records" size="small">
        <el-table-column prop="roundNo" label="轮次" width="60" />
        <el-table-column prop="workerName" label="师傅" width="90" />
        <el-table-column prop="score" label="得分" width="70" />
        <el-table-column prop="mode" label="方式" width="70" />
        <el-table-column prop="reason" label="理由 / 原因" min-width="220" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DISPATCHED' ? 'success' : 'info'" size="small">
              {{ row.status === 'DISPATCHED' ? '生效中' : '已替代' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 人工改派 -->
    <el-dialog v-model="reassignVisible" title="人工改派" width="440px">
      <el-form label-width="90px">
        <el-form-item label="改派方式">
          <el-checkbox v-model="smartReassign">智能重派（排除原师傅）</el-checkbox>
        </el-form-item>
        <el-form-item v-if="!smartReassign" label="接手师傅">
          <el-select v-model="reassignForm.workerId" placeholder="仅当日排班的师傅" style="width: 100%">
            <el-option v-for="w in workers" :key="w.workerId" :label="w.realName" :value="w.workerId" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="reassignForm.reason" type="textarea" :rows="2" placeholder="如：师傅请假 / 业主指定时间冲突" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reassignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReassign">确认改派</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.filters {
  display: flex;
  gap: 10px;
}

.muted {
  color: var(--wp-muted);
  font-size: 12px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
