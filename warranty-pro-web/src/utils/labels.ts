/** 工单状态 / 故障类别中文映射（与后端 warranty-common 枚举口径一致）。 */

export function statusLabel(status: string): string {
  const map: Record<string, string> = {
    SUBMITTED: '待受理',
    PENDING_DISPATCH: '待派单',
    EXTERNAL_PROCESSING: '物业跟进中',
    DISPATCHED: '已派单·待上门',
    IN_PROGRESS: '维修中',
    PENDING_CONFIRM: '待验收',
    COMPLETED: '已完结',
    CANCELLED: '已取消',
  }
  return map[status] ?? status
}

export function statusTagType(status: string): 'danger' | 'warning' | 'success' | 'info' | 'primary' {
  switch (status) {
    case 'SUBMITTED':
      return 'danger'
    case 'PENDING_DISPATCH':
    case 'DISPATCHED':
      return 'warning'
    case 'IN_PROGRESS':
    case 'COMPLETED':
      return 'success'
    case 'PENDING_CONFIRM':
    case 'EXTERNAL_PROCESSING':
      return 'primary'
    default:
      return 'info'
  }
}

export function categoryLabel(category: string): string {
  const map: Record<string, string> = {
    WATER_ELECTRICITY: '水电',
    CIVIL_WATERPROOF: '土建防水',
    DOOR_WINDOW: '门窗五金',
    HVAC: '暖通空调',
    ELEVATOR: '电梯设备',
    PUBLIC_FACILITY: '公共设施',
    OTHER: '其他',
  }
  return map[category] ?? category
}
