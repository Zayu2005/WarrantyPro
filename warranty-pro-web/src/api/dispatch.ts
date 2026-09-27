import request from './request'

export interface WorkerInfo {
  workerId: number
  realName: string
  skillTags: string
  todayDuty: boolean
}

export interface ScheduleRow {
  workerId: number
  dutyDate: string
}

export interface DispatchRecord {
  id: number
  roundNo: number
  mode: string
  workerId: number
  workerName: string
  score: number | null
  factors: string
  reason: string
  status: string
  createdAt: string
}

/** 师傅列表（含今日在班标记）。 */
export async function fetchWorkers(): Promise<WorkerInfo[]> {
  return (await request.get('/dispatch/workers')) as unknown as WorkerInfo[]
}

/** 排班查询（区间）。 */
export async function fetchSchedule(start: string, days = 7): Promise<ScheduleRow[]> {
  return (await request.get(`/dispatch/schedule?start=${start}&days=${days}`)) as unknown as ScheduleRow[]
}

/** 排班切换。 */
export async function toggleSchedule(workerId: number, dutyDate: string, onDuty: boolean): Promise<void> {
  await request.put('/dispatch/schedule', { workerId, dutyDate, onDuty })
}

/** 派单记录（轮次 / 得分 / 理由）。 */
export async function fetchDispatchRecords(orderId: number): Promise<DispatchRecord[]> {
  return (await request.get(`/dispatch/orders/${orderId}/records`)) as unknown as DispatchRecord[]
}

/** 人工改派：workerId 传 null 时触发智能重派（排除原师傅）。 */
export async function reassignOrder(orderId: number, workerId: number | null, reason: string): Promise<void> {
  await request.post(`/dispatch/orders/${orderId}/reassign`, { workerId, reason })
}

/** 受理工单（受理后智能体同步直派）。 */
export async function acceptOrder(orderId: number): Promise<void> {
  await request.post(`/dispatch/orders/${orderId}/accept`)
}
