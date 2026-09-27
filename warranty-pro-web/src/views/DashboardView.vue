<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref } from 'vue'

// 示例数据，待后端 /stats/overview 联调（docs/07 §2.7）
const stats = [
  { label: '今日新增工单', value: 12 },
  { label: '维修中', value: 8 },
  { label: '待验收', value: 5 },
  { label: '本月已完结', value: 96 },
]

const trendOption: echarts.EChartsOption = {
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 30, bottom: 30 },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '新增工单',
      type: 'bar',
      data: [18, 22, 15, 26, 31, 12, 9],
      itemStyle: { color: '#409eff' },
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
  <div>
    <el-row :gutter="16">
      <el-col v-for="item in stats" :key="item.label" :span="6">
        <el-card shadow="hover">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-card">
      <template #header>近 7 天工单量趋势（示例数据，待后端联调）</template>
      <div ref="chartRef" class="chart"></div>
    </el-card>
  </div>
</template>

<style scoped>
.stat-label {
  color: #909399;
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 600;
}

.chart-card {
  margin-top: 16px;
}

.chart {
  height: 320px;
}
</style>
