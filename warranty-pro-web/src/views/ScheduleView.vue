<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchSchedule, fetchWorkers, toggleSchedule } from '@/api/dispatch'
import type { WorkerInfo } from '@/api/dispatch'

// 未来 7 天排班格子：管理员按天勾选，派单候选 = 当日排班且在岗
const workers = ref<WorkerInfo[]>([])
const dutySet = ref(new Set<string>())
const loading = ref(false)

const days = computed(() => {
  const list: { date: string; label: string; weekday: string }[] = []
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  for (let i = 0; i < 7; i++) {
    const d = new Date()
    d.setDate(d.getDate() + i)
    const date = d.toISOString().substring(0, 10)
    list.push({
      date,
      label: `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
      weekday: i === 0 ? '今天' : weekdays[d.getDay()],
    })
  }
  return list
})

const key = (workerId: number, date: string) => `${workerId}|${date}`

const isDuty = (workerId: number, date: string) => dutySet.value.has(key(workerId, date))

async function load() {
  loading.value = true
  try {
    const [ws, rows] = await Promise.all([fetchWorkers(), fetchSchedule(days.value[0].date, 7)])
    workers.value = ws
    dutySet.value = new Set(rows.map((r) => key(r.workerId, r.dutyDate)))
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function onToggle(worker: WorkerInfo, date: string, checked: boolean) {
  try {
    await toggleSchedule(worker.workerId, date, checked)
    if (checked) {
      dutySet.value.add(key(worker.workerId, date))
    } else {
      dutySet.value.delete(key(worker.workerId, date))
    }
    dutySet.value = new Set(dutySet.value)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

function skillList(tags: string): string[] {
  try {
    return JSON.parse(tags) as string[]
  } catch {
    return []
  }
}

onMounted(load)
</script>

<template>
  <div class="wp-card">
    <div class="head">
      <h3 class="card-title">师傅排班（未来 7 天）</h3>
      <span class="note">派单候选 = 在岗 ∧ 当日有排班；勾选即保存</span>
    </div>

    <el-table :data="workers" v-loading="loading" border>
      <el-table-column label="师傅" min-width="180">
        <template #default="{ row }">
          <div class="worker-cell">
            <span class="worker-name">{{ row.realName }}</span>
            <el-tag v-for="t in skillList(row.skillTags)" :key="t" size="small" effect="plain" class="skill-tag">
              {{ t }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-for="d in days" :key="d.date" :label="`${d.label} ${d.weekday}`" align="center" min-width="90">
        <template #default="{ row }">
          <el-checkbox
            :model-value="isDuty(row.workerId, d.date)"
            @change="(checked: boolean | string | number) => onToggle(row, d.date, Boolean(checked))"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.note {
  font-size: 12px;
  color: var(--wp-muted);
}

.worker-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.worker-name {
  font-weight: 600;
}

.skill-tag {
  color: var(--wp-moss);
}
</style>
