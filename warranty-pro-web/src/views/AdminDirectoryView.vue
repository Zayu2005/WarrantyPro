<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { categoryLabel, statusLabel } from '@/utils/labels'

type Row = Record<string, unknown>
interface PageData { list: Row[]; total: number; page: number; pageSize: number }

const route = useRoute()
const rows = ref<Row[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', status: '', stars: '', page: 1, pageSize: 15 })

const kind = computed(() => String(route.name ?? ''))
const config = computed(() => {
  const entries: Record<string, { title: string; description: string; endpoint: string; columns: { key: string; label: string; width?: number; minWidth?: number }[] }> = {
    workers: { title: '维修人员', description: '查看维修人员在岗状态、评分与完工情况。', endpoint: '/dispatch/workers', columns: [
      { key: 'workerId', label: '人员编号', width: 100 }, { key: 'realName', label: '姓名', width: 110 }, { key: 'phone', label: '手机号', width: 140 },
      { key: 'onDuty', label: '在岗状态', width: 100 }, { key: 'todayDuty', label: '今日排班', width: 100 }, { key: 'ratingAvg', label: '平均评分', width: 100 },
      { key: 'ratingCount', label: '评价数', width: 90 }, { key: 'orderCompleted', label: '完工工单', width: 100 }, { key: 'maxConcurrent', label: '并发上限', width: 100 },
    ] },
    reviews: { title: '维修评价', description: '按评分查看业主提交的服务评价。', endpoint: '/admin/evaluations', columns: [
      { key: 'orderId', label: '工单编号', width: 100 }, { key: 'ownerId', label: '业主编号', width: 100 }, { key: 'workerId', label: '师傅编号', width: 100 },
      { key: 'stars', label: '评分', width: 90 }, { key: 'tags', label: '评价标签', minWidth: 180 }, { key: 'comment', label: '评价内容', minWidth: 260 }, { key: 'createdAt', label: '评价时间', width: 170 },
    ] },
    archives: { title: '工单归档', description: '查询已完结工单及其处理摘要。', endpoint: '/admin/archives', columns: [
      { key: 'orderNo', label: '工单号', width: 180 }, { key: 'category', label: '报修类别', width: 120 }, { key: 'locationDetail', label: '报修位置', minWidth: 180 },
      { key: 'phenomenon', label: '问题描述', minWidth: 220 }, { key: 'ownerId', label: '业主编号', width: 100 }, { key: 'currentWorkerId', label: '维修人员', width: 100 }, { key: 'completedAt', label: '完工时间', width: 170 },
    ] },
    owners: { title: '业主管理', description: '检索已注册业主账号及账号状态。', endpoint: '/admin/owners', columns: [
      { key: 'id', label: '用户编号', width: 100 }, { key: 'username', label: '登录账号', width: 150 }, { key: 'realName', label: '姓名', width: 120 },
      { key: 'phone', label: '手机号', width: 150 }, { key: 'status', label: '账号状态', width: 100 }, { key: 'createdAt', label: '注册时间', width: 180 },
    ] },
    users: { title: '用户管理', description: '查看平台账号和最近登录情况。', endpoint: '/admin/users', columns: [
      { key: 'id', label: '用户编号', width: 100 }, { key: 'username', label: '登录账号', width: 150 }, { key: 'realName', label: '姓名', width: 120 },
      { key: 'phone', label: '手机号', width: 150 }, { key: 'status', label: '账号状态', width: 100 }, { key: 'lastLoginAt', label: '最后登录', width: 180 },
    ] },
    roles: { title: '角色管理', description: '查看系统内已配置的访问角色。', endpoint: '/admin/roles', columns: [
      { key: 'id', label: '角色编号', width: 100 }, { key: 'code', label: '角色代码', width: 160 }, { key: 'name', label: '角色名称', width: 180 }, { key: 'createdAt', label: '创建时间', width: 180 },
    ] },
    notices: { title: '物业通知', description: '查看公告正文及发布状态。', endpoint: '/admin/notices', columns: [
      { key: 'title', label: '公告标题', minWidth: 220 }, { key: 'type', label: '类型', width: 120 }, { key: 'communityId', label: '小区编号', width: 100 },
      { key: 'status', label: '状态', width: 110 }, { key: 'content', label: '公告内容', minWidth: 300 }, { key: 'createdAt', label: '创建时间', width: 180 },
    ] },
  }
  return entries[kind.value] ?? entries.users
})

const isPaged = computed(() => ['reviews', 'archives', 'owners', 'users', 'notices'].includes(kind.value))

async function load() {
  loading.value = true
  try {
    const params: Record<string, string | number> = { page: query.page, pageSize: query.pageSize }
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (query.status) params.status = query.status
    if (query.stars) params.stars = Number(query.stars)
    const result = await request.get(config.value.endpoint, { params })
    if (kind.value === 'workers' || kind.value === 'roles') {
      rows.value = Array.isArray(result) ? result as Row[] : []
      total.value = rows.value.length
    } else {
      const data = result as unknown as PageData
      rows.value = data.list ?? []
      total.value = data.total ?? 0
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; load() }
function format(key: string, value: unknown): string {
  if (value === null || value === undefined || value === '') return '—'
  if (key === 'category') return categoryLabel(String(value))
  if (key === 'status' && typeof value === 'number') return value === 1 ? '启用' : '停用'
  if (key === 'status' && typeof value === 'string') return ({ PUBLISHED: '已发布', WITHDRAWN: '已撤回' } as Record<string, string>)[value] ?? statusLabel(value)
  if (key === 'onDuty' || key === 'todayDuty') return value ? (key === 'onDuty' ? '在岗' : '已排班') : (key === 'onDuty' ? '休假' : '未排班')
  if (key === 'tags' && typeof value === 'string') {
    try { return (JSON.parse(value) as string[]).join('、') || '—' } catch { return value }
  }
  if (typeof value === 'string' && value.includes('T')) return value.replace('T', ' ').slice(0, 16)
  return String(value)
}

onMounted(load)
watch(() => route.name, () => { query.keyword = ''; query.status = ''; query.stars = ''; query.page = 1; load() })
</script>

<template>
  <section class="directory">
    <header class="directory-head">
      <div>
        <h1>{{ config.title }}</h1>
        <p>{{ config.description }}</p>
      </div>
      <el-button aria-label="刷新列表" title="刷新列表" :loading="loading" @click="load">刷新</el-button>
    </header>

    <div class="directory-tools">
      <el-input v-if="['archives', 'owners', 'users', 'notices'].includes(kind)" v-model="query.keyword" clearable placeholder="搜索名称、账号、工单号" style="width: 260px" @keyup.enter="search" @clear="search" />
      <el-select v-if="kind === 'reviews'" v-model="query.stars" clearable placeholder="全部评分" style="width: 150px" @change="search">
        <el-option v-for="star in [5, 4, 3, 2, 1]" :key="star" :label="`${star} 星`" :value="String(star)" />
      </el-select>
      <el-select v-if="kind === 'notices'" v-model="query.status" clearable placeholder="全部状态" style="width: 150px" @change="search">
        <el-option label="已发布" value="PUBLISHED" /><el-option label="已撤回" value="WITHDRAWN" />
      </el-select>
      <el-button v-if="['archives', 'owners', 'users', 'notices'].includes(kind)" type="primary" @click="search">查询</el-button>
      <span class="result-count">{{ total }} 条记录</span>
    </div>

    <el-table :data="rows" v-loading="loading" row-key="id" empty-text="暂无符合条件的记录" class="data-table">
      <el-table-column v-for="column in config.columns" :key="column.key" :prop="column.key" :label="column.label" :width="column.width" :min-width="column.minWidth" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-if="['onDuty', 'todayDuty'].includes(column.key)" :type="row[column.key] ? 'success' : 'info'" size="small">{{ format(column.key, row[column.key]) }}</el-tag>
          <el-tag v-else-if="column.key === 'status' && kind !== 'archives'" :type="row.status === 1 || row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : format(column.key, row.status) }}</el-tag>
          <el-rate v-else-if="column.key === 'stars'" :model-value="Number(row.stars ?? 0)" disabled size="small" />
          <span v-else>{{ format(column.key, row[column.key]) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-if="isPaged" class="pager" background layout="total, prev, pager, next, sizes" :total="total" :page-size="query.pageSize" :current-page="query.page" :page-sizes="[15, 30, 50]" @current-change="(page: number) => { query.page = page; load() }" @size-change="(size: number) => { query.pageSize = size; query.page = 1; load() }" />
  </section>
</template>

<style scoped>
.directory { min-width: 0; }
.directory-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin: 2px 0 16px; }
h1 { margin: 0; color: #27343a; font-size: 19px; font-weight: 650; }
.directory-head p { margin: 5px 0 0; color: #77838a; font-size: 13px; }
.directory-tools { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; padding: 10px 0; border-top: 1px solid #e7ecee; }
.result-count { margin-left: auto; color: #77838a; font-size: 12px; }
.data-table { width: 100%; }
.pager { justify-content: flex-end; margin-top: 14px; }
@media (max-width: 680px) { .directory-head { align-items: center; } .directory-head p { max-width: 270px; } }
</style>
