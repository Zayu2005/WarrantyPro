import request from './request'

export interface StatusCount {
  status: string
  count: number
}

export interface TrendPoint {
  date: string
  count: number
}

export interface OverviewStats {
  pipeline: StatusCount[]
  trend: TrendPoint[]
  todayNew: number
  inProgress: number
  pendingConfirm: number
  monthCompleted: number
}

export async function fetchOverviewStats(): Promise<OverviewStats> {
  return (await request.get('/stats/overview')) as unknown as OverviewStats
}
