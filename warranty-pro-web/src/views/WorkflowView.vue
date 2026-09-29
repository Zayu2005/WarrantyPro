<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Share } from '@element-plus/icons-vue'
import { fetchWorkflowDefinition, fetchWorkflowLogs, fetchWorkflowRuns, startRepairWorkflow } from '@/api/workflow'
import type { WorkflowDefinition, WorkflowNodeLog, WorkflowRun } from '@/api/workflow'

const definition = ref<WorkflowDefinition | null>(null)
const runs = ref<WorkflowRun[]>([])
const selectedRun = ref<WorkflowRun | null>(null)
const logs = ref<WorkflowNodeLog[]>([])
const loading = ref(false)
const submitting = ref(false)
const showStartDialog = ref(false)
const workflowInput = ref({ phenomenon: '', category: '', locationDetail: '', urgency: '普通' })
let timer: number | undefined

const selectedLogMap = computed(() => new Map(logs.value.map((log) => [log.nodeKey, log])))
const nodePositions = computed(() => {
  const nodes = definition.value?.nodes ?? []
  return nodes.map((node, index) => ({ ...node, x: 84 + index * 190, y: 132 }))
})
const canvasWidth = computed(() => Math.max(900, 168 + (definition.value?.nodes.length ?? 0) * 190))
const runStateText = computed(() => selectedRun.value ? statusLabel(selectedRun.value.status) : '等待运行')

function nodeState(key: string) {
  const log = selectedLogMap.value.get(key)
  return log?.status ?? 'PENDING'
}

function nodePosition(key: string) {
  return nodePositions.value.find((node) => node.key === key)
}

function nodePoint(key: string, side: 'left' | 'right') {
  const node = nodePosition(key)
  return node ? { x: node.x + (side === 'right' ? 124 : 0), y: node.y + 30 } : { x: 0, y: 0 }
}

function edgePath(from: string, to: string) {
  const start = nodePoint(from, 'right')
  const end = nodePoint(to, 'left')
  const middle = (start.x + end.x) / 2
  return `M ${start.x} ${start.y} C ${middle} ${start.y}, ${middle} ${end.y}, ${end.x} ${end.y}`
}

function nodeClass(key: string) {
  const status = nodeState(key).toLowerCase()
  return status === 'completed' || status === 'degraded' || status === 'failed' || status === 'running'
    ? `node-${status}`
    : 'node-pending'
}

function statusLabel(status: string) {
  return ({ RUNNING: '运行中', COMPLETED: '已完成', DEGRADED: '已降级', FAILED: '失败', PENDING: '等待' } as Record<string, string>)[status] ?? status
}

function statusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'DEGRADED') return 'warning'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

async function loadRuns() {
  try {
    runs.value = await fetchWorkflowRuns()
    if (selectedRun.value) {
      const refreshed = runs.value.find((run) => run.id === selectedRun.value?.id)
      if (refreshed) selectedRun.value = refreshed
      await loadLogs()
    } else if (runs.value.length) {
      await selectRun(runs.value[0])
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载工作流运行记录失败')
  }
}

async function loadLogs() {
  if (!selectedRun.value) return
  logs.value = await fetchWorkflowLogs(selectedRun.value.id)
}

async function selectRun(run: WorkflowRun) {
  selectedRun.value = run
  try {
    await loadLogs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载节点日志失败')
  }
}

async function submitWorkflow() {
  if (!workflowInput.value.phenomenon.trim()) {
    ElMessage.warning('请先填写故障现象')
    return
  }
  submitting.value = true
  try {
    const run = await startRepairWorkflow({ ...workflowInput.value })
    showStartDialog.value = false
    workflowInput.value = { phenomenon: '', category: '', locationDetail: '', urgency: '普通' }
    await loadRuns()
    await selectRun(run)
    ElMessage.success(`已启动工作流 #${run.id}`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '启动工作流失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    definition.value = await fetchWorkflowDefinition()
    await loadRuns()
    timer = window.setInterval(loadRuns, 2000)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载工作流定义失败')
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="workflow-page" v-loading="loading">
    <header class="workflow-toolbar">
      <div class="workflow-title"><el-icon><Share /></el-icon><strong>{{ definition?.workflowName ?? '智能报修分诊工作流' }}</strong><span>{{ definition?.workflowKey }}</span></div>
      <div class="legend">
        <span><i class="legend-start"></i>起止</span><span><i class="legend-branch"></i>判断</span><span><i class="legend-agent"></i>智能体</span><span><i class="legend-tool"></i>系统环节</span><span><i class="legend-human"></i>人工环节</span><span><i class="legend-line"></i>已流转</span><span><i class="legend-dash"></i>回环分支</span>
      </div>
      <div class="toolbar-actions">
        <el-select v-model="selectedRun" value-key="id" placeholder="选择运行记录" class="run-select" @change="selectRun">
          <el-option v-for="run in runs" :key="run.id" :label="`#${run.id} · ${run.inputSummary || '报修分诊'}`" :value="run" />
        </el-select>
        <el-button type="primary" @click="showStartDialog = true">启动分诊</el-button>
      </div>
    </header>

    <section class="canvas-section" aria-label="工作流流程图">
      <div class="flow-canvas">
        <svg v-if="definition" class="flow-svg" :viewBox="`0 0 ${canvasWidth} 264`" :style="{ minWidth: `${canvasWidth}px` }" role="img" :aria-label="definition.workflowName">
          <defs>
            <pattern id="flow-grid" width="22" height="22" patternUnits="userSpaceOnUse"><circle cx="1" cy="1" r=".8" fill="#34363c" /></pattern>
            <marker id="flow-arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#696c75" /></marker>
            <marker id="flow-arrow-active" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#42b883" /></marker>
          </defs>
          <rect width="100%" height="100%" fill="url(#flow-grid)" />
          <g v-for="(edge, index) in definition.edges" :key="`${edge.from}-${edge.to}-${index}`">
            <path :d="edgePath(edge.from, edge.to)" class="flow-edge" :class="{ passed: !!selectedLogMap.get(edge.from) && !!selectedLogMap.get(edge.to) }" :marker-end="selectedLogMap.get(edge.to) ? 'url(#flow-arrow-active)' : 'url(#flow-arrow)'" />
            <text v-if="edge.label" :x="(nodePoint(edge.from, 'right').x + nodePoint(edge.to, 'left').x) / 2" y="112" class="edge-label">{{ edge.label }}</text>
          </g>
          <g v-for="node in nodePositions" :key="node.key" class="flow-node" :class="nodeClass(node.key)" :transform="`translate(${node.x}, ${node.y})`">
            <rect class="node-box" width="124" height="60" rx="7" />
            <circle class="node-dot" cx="15" cy="17" r="4" />
            <text x="25" y="21" class="node-title">{{ node.name }}</text>
            <text x="12" y="43" class="node-subtitle">{{ node.type }} · {{ statusLabel(nodeState(node.key)) }}</text>
          </g>
          <text v-if="!runs.length" x="22" y="230" class="canvas-hint">启动一次报修分诊后，可在此查看节点状态与流转记录</text>
        </svg>
      </div>
      <div class="canvas-footer"><span>{{ definition?.nodes.length ?? 0 }} 个节点</span><span>{{ definition?.edges.length ?? 0 }} 条流转关系</span><span>{{ selectedRun ? `运行 #${selectedRun.id}` : '未选择运行记录' }}</span><span class="run-state" :class="`status-${selectedRun?.status.toLowerCase() ?? 'pending'}`">{{ runStateText }}</span><span class="canvas-spacer"></span><el-button text aria-label="放大流程图">⌕</el-button><el-button text aria-label="适应画布">□</el-button></div>
    </section>

    <section class="console-section">
      <header class="console-header"><div><strong>实时运行日志</strong><span v-if="selectedRun">工单 #{{ selectedRun.id }} · 场景：{{ selectedRun.inputSummary }} · 耗时 {{ selectedRun.latencyMs ?? '—' }} ms</span><span v-else>选择或启动一个工作流以查看动态调用日志</span></div><el-tag v-if="selectedRun" :type="statusType(selectedRun.status)" effect="plain" size="small">{{ statusLabel(selectedRun.status) }}</el-tag><span v-else class="live-indicator"><i></i>轮询中</span></header>
      <div v-if="logs.length" class="log-console" role="log" aria-live="polite">
        <div v-for="log in logs" :key="log.id" class="console-row" :class="`console-${log.status.toLowerCase()}`">
          <time>{{ log.startedAt?.slice(11, 19) ?? '--:--:--' }}</time><span class="console-name">{{ log.nodeName }}</span><span class="console-model">{{ log.model || '规则节点' }}</span><span class="console-output">{{ log.outputSummary || log.errorMessage || '节点正在执行…' }}</span><span class="console-latency">{{ log.latencyMs ?? '…' }} ms</span>
        </div>
      </div>
      <div v-else class="console-empty">暂无调用日志</div>
    </section>

    <section class="runs-strip"><div class="runs-strip-title">最近运行 <span>{{ runs.length }}</span></div><button v-for="run in runs.slice(0, 8)" :key="run.id" class="run-chip" :class="{ active: selectedRun?.id === run.id }" @click="selectRun(run)"><i :class="`status-dot status-${run.status.toLowerCase()}`"></i><span>#{{ run.id }} · {{ run.inputSummary || '报修分诊' }}</span><small>{{ statusLabel(run.status) }}</small></button><span v-if="!runs.length" class="no-runs">等待首次运行</span></section>

    <el-dialog v-model="showStartDialog" title="启动报修分诊" width="520px">
      <el-form label-position="top">
        <el-form-item label="故障现象" required>
          <el-input v-model="workflowInput.phenomenon" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="例如：卫生间顶部持续滴水，晚上更严重" />
        </el-form-item>
        <div class="dialog-form-grid">
          <el-form-item label="故障类别">
            <el-input v-model="workflowInput.category" maxlength="60" placeholder="例如：土建防水" />
          </el-form-item>
          <el-form-item label="紧急程度">
            <el-select v-model="workflowInput.urgency" class="full-width">
              <el-option label="普通" value="普通" />
              <el-option label="紧急" value="紧急" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="具体位置">
          <el-input v-model="workflowInput.locationDetail" maxlength="200" placeholder="例如：3 栋 2 单元 1202 主卫" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showStartDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitWorkflow">启动工作流</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.workflow-page { display: grid; grid-template-rows: auto minmax(290px, 1fr) minmax(170px, .62fr) auto; gap: 9px; min-height: calc(100vh - 112px); color: #303238; }
.workflow-toolbar { display: flex; align-items: center; gap: 14px; min-height: 38px; padding: 0 3px; }
.workflow-title { display: flex; align-items: center; gap: 8px; white-space: nowrap; }
.workflow-title :deep(.el-icon) { color: #a660bf; font-size: 17px; }
.workflow-title strong { font-size: 15px; }
.workflow-title > span { color: #9a9ca3; font-size: 11px; }
.legend { display: flex; flex: 1; justify-content: flex-end; gap: 12px; color: #6f727a; font-size: 10px; white-space: nowrap; }
.legend span { display: inline-flex; align-items: center; gap: 4px; }
.legend i { display: inline-block; width: 8px; height: 8px; border: 1px solid #8cceb0; border-radius: 50%; }
.legend-branch { border-color: #bd8cd2 !important; transform: rotate(45deg); border-radius: 1px !important; }
.legend-agent { background: #1687ed; border-color: #1687ed !important; }
.legend-tool { background: #92949b; border-color: #92949b !important; }
.legend-human { background: #e8a21b; border-color: #e8a21b !important; }
.legend-line { width: 15px !important; height: 2px !important; border: 0 !important; border-radius: 0 !important; background: #1687ed; }
.legend-dash { width: 15px !important; height: 0 !important; border: 0 !important; border-top: 2px dashed #df9a20 !important; border-radius: 0 !important; }
.toolbar-actions { display: flex; align-items: center; gap: 8px; }
.run-select { width: 230px; }
.canvas-section { display: flex; min-height: 0; flex-direction: column; border: 1px solid #e6e7ea; background: #fff; }
.flow-canvas { flex: 1; min-height: 0; overflow: auto; background: #202126; }
.flow-svg { display: block; width: 100%; height: 100%; min-height: 292px; }
.flow-edge { fill: none; stroke: #666972; stroke-width: 1.5; marker-end: url(#flow-arrow); transition: stroke .2s; }
.flow-edge.passed { stroke: #37aa77; stroke-width: 2; marker-end: url(#flow-arrow-active); }
.edge-label { fill: #aaaeb7; font-size: 9px; text-anchor: middle; }
.node-box { fill: #303137; stroke: #45474f; stroke-width: 1; }
.flow-node.node-completed .node-box { fill: #102f28; stroke: #279b69; }
.flow-node.node-degraded .node-box { fill: #342a17; stroke: #d1982d; }
.flow-node.node-failed .node-box { fill: #3a2023; stroke: #d65c62; }
.flow-node.node-running .node-box { fill: #0d2f50; stroke: #2686d2; stroke-width: 1.5; }
.node-dot { fill: #8b8d95; }
.node-completed .node-dot { fill: #39ba80; }
.node-degraded .node-dot { fill: #e9a72d; }
.node-failed .node-dot { fill: #ef626a; }
.node-running .node-dot { fill: #2494f0; }
.node-title { fill: #f0f1f3; font-size: 11px; font-weight: 600; }
.node-subtitle { fill: #a9abb2; font-size: 9px; }
.canvas-hint { fill: #7f818a; font-size: 11px; }
.canvas-footer { display: flex; align-items: center; gap: 16px; min-height: 33px; padding: 0 10px; color: #747780; font-size: 10px; }
.run-state { padding: 3px 7px; border-radius: 3px; background: #f0f1f2; color: #747780; }
.status-running { background: #fff3dc; color: #b97912; }
.status-completed { background: #e9f7ef; color: #258252; }
.status-degraded { background: #fff3dc; color: #b97912; }
.status-failed { background: #fff0f0; color: #c63e45; }
.canvas-spacer { flex: 1; }
.console-section { display: flex; min-height: 0; flex-direction: column; border: 1px solid #e6e7ea; background: #fff; }
.console-header { display: flex; align-items: center; justify-content: space-between; gap: 14px; min-height: 40px; padding: 0 10px; border-bottom: 1px solid #e9eaed; }
.console-header > div { display: flex; align-items: baseline; gap: 12px; min-width: 0; }
.console-header strong { flex: 0 0 auto; font-size: 13px; }
.console-header span { overflow: hidden; color: #898b93; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.live-indicator { display: inline-flex; align-items: center; gap: 5px; }
.live-indicator i { width: 7px; height: 7px; border-radius: 50%; background: #37ad73; }
.log-console { flex: 1; overflow: auto; padding: 5px 9px; background: #202126; color: #d5d7dc; font: 11px/1.55 Consolas, 'SFMono-Regular', monospace; }
.console-row { display: grid; grid-template-columns: 66px 130px 130px minmax(160px, 1fr) 52px; align-items: baseline; gap: 8px; min-height: 24px; border-bottom: 1px solid #2b2c32; }
.console-row time, .console-latency { color: #8d9099; font-variant-numeric: tabular-nums; }
.console-name { color: #62b7f0; }
.console-model { color: #9f83d0; }
.console-output { overflow: hidden; color: #bec1c8; text-overflow: ellipsis; white-space: nowrap; }
.console-degraded .console-name { color: #e9aa39; }
.console-failed .console-name { color: #f1777e; }
.console-completed .console-name { color: #48c28a; }
.console-empty { display: grid; flex: 1; min-height: 96px; place-items: center; background: #202126; color: #858891; font-size: 12px; }
.runs-strip { display: flex; align-items: center; gap: 7px; min-width: 0; overflow-x: auto; padding: 1px 2px; }
.runs-strip-title { flex: 0 0 auto; margin-right: 4px; color: #686b73; font-size: 11px; font-weight: 600; }
.runs-strip-title span { margin-left: 4px; color: #a1a3aa; font-weight: 400; }
.run-chip { display: inline-flex; align-items: center; gap: 6px; flex: 0 0 auto; max-width: 240px; height: 27px; padding: 0 8px; border: 1px solid #e6e7ea; border-radius: 4px; background: #fff; color: #555861; cursor: pointer; }
.run-chip.active { border-color: #c89bd8; background: #fbf6fd; }
.run-chip > span { overflow: hidden; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.run-chip small { color: #9698a0; font-size: 9px; white-space: nowrap; }
.status-dot { width: 7px; height: 7px; flex: 0 0 auto; border-radius: 50%; background: #9b9da4; }
.status-dot.status-running { background: #e7a32a; }
.status-dot.status-completed { background: #35aa72; }
.status-dot.status-degraded { background: #e7a32a; }
.status-dot.status-failed { background: #d95259; }
.no-runs { color: #93959c; font-size: 11px; }
.dialog-form-grid { display: grid; grid-template-columns: 1fr 140px; gap: 14px; }
.full-width { width: 100%; }
@media (max-width: 1100px) { .workflow-page { grid-template-rows: auto minmax(280px, 1fr) minmax(160px, .6fr) auto; } .legend { display: none; } .workflow-toolbar { justify-content: space-between; } }
@media (max-width: 650px) { .workflow-page { min-height: calc(100vh - 105px); grid-template-rows: auto minmax(260px, 1fr) minmax(150px, .6fr) auto; } .workflow-toolbar { flex-wrap: wrap; } .workflow-title { width: 100%; } .toolbar-actions { width: 100%; } .run-select { flex: 1; min-width: 0; } .console-row { grid-template-columns: 58px 95px minmax(120px, 1fr) 45px; } .console-model { display: none; } .dialog-form-grid { grid-template-columns: 1fr; gap: 0; } }
</style>
