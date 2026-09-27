<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchSchedule, fetchWorkers, toggleSchedule } from '@/api/dispatch'
import type { WorkerInfo } from '@/api/dispatch'

// 排班日历：月历格子展示每日值班师傅，点选日期后在右侧切换值班人
const workers = ref<WorkerInfo[]>([])
const dutySet = ref(new Set<string>())
const loading = ref(false)
const current = ref(new Date())
const selected = ref(fmt(new Date()))

function fmt(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const monthLabel = computed(() => `${current.value.getFullYear()} 年 ${current.value.getMonth() + 1} 月`)

/** 42 格月历（周一为首列，含前后月补位）。 */
const cells = computed(() => {
  const y = current.value.getFullYear()
  const m = current.value.getMonth()
  const first = new Date(y, m, 1)
  const offset = (first.getDay() + 6) % 7
  const list: { date: string; day: number; inMonth: boolean; isToday: boolean }[] = []
  for (let i = 0; i < 42; i++) {
    const d = new Date(y, m, 1 - offset + i)
    list.push({ date: fmt(d), day: d.getDate(), inMonth: d.getMonth() === m, isToday: fmt(d) === fmt(new Date()) })
  }
  return list
})

const key = (workerId: number, date: string) => `${workerId}|${date}`

function dutyWorkers(date: string): WorkerInfo[] {
  return workers.value.filter((w) => dutySet.value.has(key(w.workerId, date)))
}

const selectedLabel = computed(() => {
  const d = new Date(`${selected.value}T00:00:00`)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${selected.value} ${weekdays[d.getDay()]}`
})

function shiftMonth(delta: number) {
  current.value = new Date(current.value.getFullYear(), current.value.getMonth() + delta, 1)
  load()
}

async function load() {
  loading.value = true
  try {
    const firstCell = cells.value[0].date
    const [ws, rows] = await Promise.all([fetchWorkers(), fetchSchedule(firstCell, 42)])
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
  <div class="schedule-page">
    <!-- 月历 -->
    <div class="wp-card calendar-card">
      <div class="cal-head">
        <div class="cal-nav">
          <el-button size="small" circle @click="shiftMonth(-1)">‹</el-button>
          <span class="cal-title">{{ monthLabel }}</span>
          <el-button size="small" circle @click="shiftMonth(1)">›</el-button>
        </div>
        <el-button size="small" text type="primary" @click="current = new Date(); selected = fmt(new Date()); load()">
          回到本月
        </el-button>
      </div>

      <div class="cal-grid">
        <div v-for="w in ['一', '二', '三', '四', '五', '六', '日']" :key="w" class="cal-weekday">{{ w }}</div>
        <div
          v-for="c in cells"
          :key="c.date"
          class="cal-cell"
          :class="{ out: !c.inMonth, today: c.isToday, selected: c.date === selected }"
          @click="selected = c.date"
        >
          <div class="cal-day">{{ c.day }}</div>
          <div class="cal-names">
            <span v-for="w in dutyWorkers(c.date).slice(0, 2)" :key="w.workerId" class="cal-chip">{{ w.realName }}</span>
            <span v-if="dutyWorkers(c.date).length > 2" class="cal-more">+{{ dutyWorkers(c.date).length - 2 }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 选中日期的值班编辑 -->
    <div class="wp-card duty-panel">
      <h3 class="card-title">{{ selectedLabel }} 值班</h3>
      <p class="duty-note">切换开关即保存；派单候选 = 在岗 ∧ 当日排班（不限技能领域）</p>
      <div v-if="workers.length === 0" class="duty-empty" v-loading="loading">暂无师傅档案</div>
      <div v-for="w in workers" :key="w.workerId" class="duty-row">
        <div class="duty-worker">
          <span class="duty-name">{{ w.realName }}</span>
          <el-tag v-for="t in skillList(w.skillTags)" :key="t" size="small" effect="plain" class="duty-tag">
            {{ t }}
          </el-tag>
        </div>
        <el-switch
          :model-value="dutySet.has(key(w.workerId, selected))"
          @change="(v: boolean | string | number) => onToggle(w, selected, Boolean(v))"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.schedule-page {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.calendar-card {
  flex: 1;
  min-width: 520px;
}

.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.cal-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cal-title {
  font-size: 15px;
  font-weight: 600;
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.cal-weekday {
  text-align: center;
  font-size: 12px;
  color: var(--wp-muted);
  padding: 4px 0;
}

.cal-cell {
  min-height: 72px;
  padding: 6px;
  border-radius: 10px;
  border: 1px solid var(--wp-hairline);
  cursor: pointer;
  transition: border-color 0.15s;
}

.cal-cell:hover {
  border-color: var(--wp-moss);
}

.cal-cell.out {
  opacity: 0.45;
}

.cal-cell.today {
  border-color: var(--wp-amber);
}

.cal-cell.selected {
  background: rgba(46, 109, 103, 0.08);
  border-color: var(--wp-moss);
}

.cal-day {
  font-size: 13px;
  font-weight: 600;
  color: var(--wp-ink);
}

.cal-names {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  margin-top: 4px;
}

.cal-chip {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  background: rgba(46, 109, 103, 0.12);
  color: var(--wp-moss);
}

.cal-more {
  font-size: 10px;
  color: var(--wp-muted);
}

.duty-panel {
  width: 320px;
  flex-shrink: 0;
}

.duty-note {
  margin: 6px 0 12px;
  font-size: 12px;
  color: var(--wp-muted);
}

.duty-empty {
  padding: 24px 0;
  text-align: center;
  color: var(--wp-muted);
  font-size: 13px;
}

.duty-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--wp-hairline);
}

.duty-row:last-child {
  border-bottom: none;
}

.duty-worker {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.duty-name {
  font-weight: 600;
}

.duty-tag {
  color: var(--wp-moss);
}
</style>
