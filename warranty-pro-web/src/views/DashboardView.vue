<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchOverviewStats } from '@/api/stats'
import type { OverviewStats, TrendPoint } from '@/api/stats'

const loading = ref(true)
const stats = ref<OverviewStats>({
  pipeline: [], trend: [], todayNew: 0, inProgress: 0, pendingConfirm: 0, monthCompleted: 0,
})
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined

const pipelineMeta: Record<string, { label: string; color: string }> = {
  SUBMITTED: { label: '待受理', color: '#C4472E' },
  PENDING_DISPATCH: { label: '待派单', color: '#E8930C' },
  DISPATCHED: { label: '已派单', color: '#D49A27' },
  IN_PROGRESS: { label: '维修中', color: '#2E6D67' },
  PENDING_CONFIRM: { label: '待验收', color: '#5B7A9D' },
  COMPLETED: { label: '已完结', color: '#8A968F' },
  EXTERNAL_PROCESSING: { label: '物业跟进', color: '#7B6BA8' },
}

const pipeline = () => stats.value.pipeline
  .filter((item) => pipelineMeta[item.status])
  .map((item) => ({ ...item, ...pipelineMeta[item.status] }))

const statCards = () => [
  { label: '今日新增', value: stats.value.todayNew, tick: '#103B3A' },
  { label: '维修中', value: stats.value.inProgress, tick: '#2E6D67' },
  { label: '待验收', value: stats.value.pendingConfirm, tick: '#E8930C' },
  { label: '本月已完结', value: stats.value.monthCompleted, tick: '#8A968F' },
]

function recentDates(points: TrendPoint[]): TrendPoint[] {
  const values = new Map(points.map((point) => [point.date, point.count]))
  const result: TrendPoint[] = []
  const localDate = (date: Date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  for (let i = 6; i >= 0; i -= 1) {
    const date = new Date()
    date.setHours(0, 0, 0, 0)
    date.setDate(date.getDate() - i)
    const key = localDate(date)
    result.push({ date: key, count: values.get(key) ?? 0 })
  }
  return result
}

function renderChart() {
  if (!chartRef.value) return
  chart ??= echarts.init(chartRef.value)
  const trend = recentDates(stats.value.trend)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 28 },
    xAxis: {
      type: 'category', data: trend.map((point) => point.date.slice(5)),
      axisLine: { lineStyle: { color: '#DCE7E3' } }, axisTick: { show: false },
      axisLabel: { color: '#8A968F' },
    },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#8A968F' }, splitLine: { lineStyle: { color: '#EDF2F0' } } },
    series: [{ name: '新增工单', type: 'bar', data: trend.map((point) => point.count), itemStyle: { color: '#2E6D67', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 26 }],
  })
}

async function load() {
  loading.value = true
  try {
    stats.value = await fetchOverviewStats()
    renderChart()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '统计数据加载失败')
  } finally {
    loading.value = false
  }
}

function onResize() { chart?.resize() }

onMounted(() => { load(); window.addEventListener('resize', onResize) })
onBeforeUnmount(() => { window.removeEventListener('resize', onResize); chart?.dispose() })
</script>

<template>
  <div class="dash" v-loading="loading">
    <section class="wp-card overview">
      <div class="ov-head"><h3 class="card-title">今日工单流水线</h3><span class="mock-note">实时数据</span></div>
      <div v-if="pipeline().length" class="bar">
        <div v-for="seg in pipeline()" :key="seg.status" class="seg" :style="{ width: `${Math.max(seg.count, 1) / Math.max(pipeline().reduce((sum, item) => sum + item.count, 0), 1) * 100}%`, background: seg.color }"></div>
      </div>
      <div class="legend">
        <span v-for="seg in pipeline()" :key="seg.status" class="lg-item"><i :style="{ background: seg.color }"></i>{{ seg.label }} <b class="wp-num">{{ seg.count }}</b></span>
        <span v-if="!pipeline().length" class="mock-note">暂无工单数据</span>
      </div>
      <div class="stat-row">
        <div v-for="item in statCards()" :key="item.label" class="stat">
          <span class="stat-tick" :style="{ background: item.tick }"></span>
          <div><div class="stat-value wp-num">{{ item.value }}</div><div class="stat-label">{{ item.label }}</div></div>
        </div>
      </div>
    </section>
    <section class="wp-card">
      <div class="ov-head"><h3 class="card-title">近 7 天工单量</h3><span class="mock-note">实时数据</span></div>
      <div ref="chartRef" class="chart"></div>
    </section>
  </div>
</template>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 16px; }
.ov-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.mock-note { font-size: 12px; color: var(--wp-muted); }
.bar { display: flex; gap: 2px; height: 10px; border-radius: 6px; overflow: hidden; }
.seg { height: 100%; }
.legend { display: flex; flex-wrap: wrap; gap: 6px 18px; margin-top: 12px; }
.lg-item { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--wp-muted); }
.lg-item i { width: 8px; height: 8px; border-radius: 2px; }
.lg-item b { color: var(--wp-ink); font-weight: 600; }
.stat-row { display: flex; margin-top: 18px; padding-top: 18px; border-top: 1px solid var(--wp-hairline); }
.stat { flex: 1; display: flex; align-items: center; gap: 12px; }
.stat + .stat { border-left: 1px solid var(--wp-hairline); padding-left: 20px; }
.stat-tick { width: 3px; height: 32px; border-radius: 2px; }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.1; }
.stat-label { margin-top: 3px; font-size: 12px; color: var(--wp-muted); }
.chart { height: 300px; }
</style>
