<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref } from 'vue'

// 示例数据，待后端 /stats/overview 联调（docs/07 §2.7）
const pipeline = [
  { label: '待受理', count: 3, color: '#C4472E', pct: 9 },
  { label: '待派单', count: 5, color: '#E8930C', pct: 14 },
  { label: '维修中', count: 14, color: '#2E6D67', pct: 40 },
  { label: '待验收', count: 4, color: '#5B7A9D', pct: 12 },
  { label: '已完结', count: 9, color: '#DCE7E3', pct: 25 },
]

const stats = [
  { label: '今日新增', value: 12, tick: '#103B3A' },
  { label: '维修中', value: 8, tick: '#2E6D67' },
  { label: '待验收', value: 5, tick: '#E8930C' },
  { label: '本月已完结', value: 96, tick: '#8A968F' },
]

const trendOption: echarts.EChartsOption = {
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 28 },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    axisLine: { lineStyle: { color: '#DCE7E3' } },
    axisTick: { show: false },
    axisLabel: { color: '#8A968F' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#8A968F' },
    splitLine: { lineStyle: { color: '#EDF2F0' } },
  },
  series: [
    {
      name: '新增工单',
      type: 'bar',
      data: [18, 22, 15, 26, 31, 12, 9],
      itemStyle: { color: '#2E6D67', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 26,
    },
  ],
}

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined

function onResize() {
  chart?.resize()
}

onMounted(() => {
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
    chart.setOption(trendOption)
    window.addEventListener('resize', onResize)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<template>
  <div class="dash">
    <!-- 签名卡：今日工单流水线（鸿蒙端进度条的工作台呼应） -->
    <section class="wp-card overview">
      <div class="ov-head">
        <h3 class="card-title">今日工单流水线</h3>
        <span class="mock-note">示例数据 · 待后端联调</span>
      </div>

      <div class="bar">
        <div
          v-for="seg in pipeline"
          :key="seg.label"
          class="seg"
          :style="{ width: seg.pct + '%', background: seg.color }"
        ></div>
      </div>
      <div class="legend">
        <span v-for="seg in pipeline" :key="seg.label" class="lg-item">
          <i :style="{ background: seg.color }"></i>{{ seg.label }}
          <b class="wp-num">{{ seg.count }}</b>
        </span>
      </div>

      <div class="stat-row">
        <div v-for="s in stats" :key="s.label" class="stat">
          <span class="stat-tick" :style="{ background: s.tick }"></span>
          <div>
            <div class="stat-value wp-num">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <section class="wp-card">
      <div class="ov-head">
        <h3 class="card-title">近 7 天工单量</h3>
        <span class="mock-note">示例数据 · 待后端联调</span>
      </div>
      <div ref="chartRef" class="chart"></div>
    </section>
  </div>
</template>

<style scoped>
.dash {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ov-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.mock-note {
  font-size: 12px;
  color: var(--wp-muted);
}

.bar {
  display: flex;
  gap: 2px;
  height: 10px;
  border-radius: 6px;
  overflow: hidden;
}

.seg {
  height: 100%;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  margin-top: 12px;
}

.lg-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--wp-muted);
}

.lg-item i {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.lg-item b {
  color: var(--wp-ink);
  font-weight: 600;
}

.stat-row {
  display: flex;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--wp-hairline);
}

.stat {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat + .stat {
  border-left: 1px solid var(--wp-hairline);
  padding-left: 20px;
}

.stat-tick {
  width: 3px;
  height: 32px;
  border-radius: 2px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
}

.stat-label {
  margin-top: 3px;
  font-size: 12px;
  color: var(--wp-muted);
}

.chart {
  height: 300px;
}
</style>
