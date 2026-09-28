<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
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

function nodeState(key: string) {
  const log = selectedLogMap.value.get(key)
  return log?.status ?? 'PENDING'
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
    <div class="workflow-head">
      <div>
        <p class="eyebrow">AGENT WORKFLOW</p>
        <h2>智能体工作流</h2>
        <p class="subhead">LangChain4j 报修分诊链路 · 节点状态和调用日志实时刷新</p>
      </div>
      <el-button type="primary" @click="showStartDialog = true">启动报修分诊</el-button>
    </div>

    <section class="wp-card flow-card">
      <div class="section-head">
        <h3 class="card-title">{{ definition?.workflowName ?? '智能报修分诊工作流' }}</h3>
        <span class="muted">{{ definition?.workflowKey }}</span>
      </div>
      <div v-if="definition" class="flow-track">
        <template v-for="(node, index) in definition.nodes" :key="node.key">
          <div class="flow-node" :class="`state-${nodeState(node.key).toLowerCase()}`">
            <div class="node-order">{{ node.order }}</div>
            <div class="node-name">{{ node.name }}</div>
            <div class="node-type">{{ node.type }}</div>
            <el-tag :type="statusType(nodeState(node.key))" size="small" effect="plain">{{ statusLabel(nodeState(node.key)) }}</el-tag>
          </div>
          <div v-if="index < definition.nodes.length - 1" class="flow-arrow">→</div>
        </template>
      </div>
    </section>

    <div class="workflow-grid">
      <section class="wp-card runs-card">
        <div class="section-head">
          <h3 class="card-title">运行记录</h3>
          <span class="muted">{{ runs.length }} 条</span>
        </div>
        <div v-if="!runs.length" class="empty">暂无运行记录</div>
        <button v-for="run in runs" :key="run.id" class="run-row" :class="{ active: selectedRun?.id === run.id }" @click="selectRun(run)">
          <span class="run-id">#{{ run.id }}</span>
          <span class="run-summary">{{ run.inputSummary || '无输入摘要' }}</span>
          <el-tag :type="statusType(run.status)" size="small" effect="plain">{{ statusLabel(run.status) }}</el-tag>
        </button>
      </section>

      <section class="wp-card logs-card">
        <div class="section-head">
          <div>
            <h3 class="card-title">动态调用日志</h3>
            <span v-if="selectedRun" class="muted">运行 #{{ selectedRun.id }} · {{ selectedRun.latencyMs ?? '—' }} ms</span>
          </div>
          <el-tag v-if="selectedRun" :type="statusType(selectedRun.status)" size="small">{{ statusLabel(selectedRun.status) }}</el-tag>
        </div>
        <el-timeline v-if="logs.length">
          <el-timeline-item v-for="log in logs" :key="log.id" :timestamp="`${log.latencyMs ?? '—'} ms`" placement="top" :type="log.status === 'FAILED' ? 'danger' : log.status === 'DEGRADED' ? 'warning' : 'primary'">
            <div class="log-head"><strong>{{ log.sequenceNo }}. {{ log.nodeName }}</strong><el-tag :type="statusType(log.status)" size="small" effect="plain">{{ statusLabel(log.status) }}</el-tag></div>
            <div class="log-meta">{{ log.model || '规则节点' }} · {{ log.nodeKey }}</div>
            <p>{{ log.outputSummary || log.errorMessage || '节点已启动，等待输出' }}</p>
          </el-timeline-item>
        </el-timeline>
        <div v-else class="empty">选择运行记录后查看节点调用日志</div>
      </section>
    </div>

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
.workflow-page { display: grid; gap: 16px; }
.workflow-head, .section-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.workflow-head h2 { margin: 2px 0 6px; font-size: 24px; }
.eyebrow { margin: 0; color: var(--wp-amber); font-size: 11px; letter-spacing: 1.4px; font-weight: 700; }
.subhead, .muted, .log-meta { color: var(--wp-muted); font-size: 12px; }
.subhead { margin: 0; }
.flow-card { overflow-x: auto; }
.flow-track { display: flex; align-items: center; min-width: 900px; padding: 20px 4px 4px; }
.flow-node { width: 132px; min-height: 124px; padding: 12px; border: 1px solid var(--wp-mist); border-radius: 10px; background: #fbfdfc; text-align: center; transition: border-color .2s, background .2s; }
.flow-node.state-running { border-color: var(--wp-amber); background: #fff9ef; }
.flow-node.state-completed { border-color: #77b6a9; background: #f2fbf8; }
.flow-node.state-degraded { border-color: #e5bd73; background: #fff9ef; }
.flow-node.state-failed { border-color: var(--wp-brick); background: #fff5f2; }
.node-order { width: 24px; height: 24px; margin: 0 auto 8px; border-radius: 50%; background: var(--wp-ink); color: white; line-height: 24px; font-size: 12px; }
.node-name { font-size: 14px; font-weight: 600; }
.node-type { margin: 5px 0 10px; color: var(--wp-muted); font-size: 10px; letter-spacing: .7px; }
.flow-arrow { flex: 1; min-width: 28px; color: var(--wp-moss); font-size: 22px; text-align: center; }
.workflow-grid { display: grid; grid-template-columns: minmax(280px, .8fr) minmax(440px, 1.6fr); gap: 16px; }
.runs-card, .logs-card { min-height: 360px; }
.run-row { width: 100%; display: grid; grid-template-columns: 45px 1fr auto; gap: 8px; align-items: center; padding: 12px 8px; border: 0; border-bottom: 1px solid var(--wp-hairline); background: transparent; color: var(--wp-ink); text-align: left; cursor: pointer; }
.run-row:hover, .run-row.active { background: #f0f7f4; }
.run-id { color: var(--wp-moss); font-size: 12px; font-variant-numeric: tabular-nums; }
.run-summary { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.empty { padding: 42px 12px; color: var(--wp-muted); text-align: center; font-size: 13px; }
.log-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.log-meta { margin-top: 4px; }
.logs-card :deep(.el-timeline) { margin-top: 18px; }
.logs-card p { margin: 7px 0 0; color: var(--wp-ink); line-height: 1.6; font-size: 13px; }
.dialog-form-grid { display: grid; grid-template-columns: 1fr 140px; gap: 14px; }
.full-width { width: 100%; }
@media (max-width: 900px) { .workflow-grid { grid-template-columns: 1fr; } }
@media (max-width: 560px) { .dialog-form-grid { grid-template-columns: 1fr; gap: 0; } }
</style>
