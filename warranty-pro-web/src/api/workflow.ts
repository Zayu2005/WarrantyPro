import request from './request'

export interface WorkflowNode {
  key: string
  name: string
  type: string
  order: number
}

export interface WorkflowEdge {
  from: string
  to: string
  label: string
}

export interface WorkflowDefinition {
  workflowKey: string
  workflowName: string
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
}

export interface WorkflowRun {
  id: number
  workflowKey: string
  workflowName: string
  callerId: number | null
  inputSummary: string
  status: string
  result: string | null
  latencyMs: number | null
  startedAt: string
  finishedAt: string | null
  createdAt: string
}

export interface WorkflowNodeLog {
  id: number
  runId: number
  nodeKey: string
  nodeName: string
  sequenceNo: number
  status: string
  inputSummary: string
  outputSummary: string | null
  model: string | null
  latencyMs: number | null
  errorMessage: string | null
  startedAt: string
  finishedAt: string | null
  createdAt: string
}

export interface RepairWorkflowInput {
  phenomenon: string
  category?: string
  locationDetail?: string
  urgency?: string
}

export async function fetchWorkflowDefinition(): Promise<WorkflowDefinition> {
  return (await request.get('/agent/workflows/definition')) as unknown as WorkflowDefinition
}

export async function fetchWorkflowRuns(limit = 30): Promise<WorkflowRun[]> {
  return (await request.get(`/agent/workflows/runs?limit=${limit}`)) as unknown as WorkflowRun[]
}

export async function fetchWorkflowLogs(runId: number): Promise<WorkflowNodeLog[]> {
  return (await request.get(`/agent/workflows/runs/${runId}/logs`)) as unknown as WorkflowNodeLog[]
}

export async function startRepairWorkflow(input: RepairWorkflowInput): Promise<WorkflowRun> {
  return (await request.post('/agent/workflows/repair-triage/runs', input)) as unknown as WorkflowRun
}
