/** 工单状态 / 故障类别的中文标签与品牌色（与 warranty-common 枚举口径一致）。 */

export function statusLabel(status: string): string {
  switch (status) {
    case 'SUBMITTED':
      return '待受理';
    case 'PENDING_DISPATCH':
      return '待派单';
    case 'EXTERNAL_PROCESSING':
      return '物业跟进中';
    case 'DISPATCHED':
      return '已派单·待上门';
    case 'IN_PROGRESS':
      return '维修中';
    case 'PENDING_CONFIRM':
      return '待验收';
    case 'COMPLETED':
      return '已完结';
    case 'CANCELLED':
      return '已取消';
    default:
      return status;
  }
}

/** 状态色：待受理砖红（需关注）/ 派单链琥珀 / 处理链苔青 / 终态弱灰 */
export function statusColor(status: string): string {
  switch (status) {
    case 'SUBMITTED':
      return '#C4472E';
    case 'PENDING_DISPATCH':
    case 'DISPATCHED':
      return '#E8930C';
    case 'EXTERNAL_PROCESSING':
      return '#5B7A9D';
    case 'IN_PROGRESS':
      return '#2E6D67';
    case 'PENDING_CONFIRM':
      return '#5B7A9D';
    default:
      return '#8A968F';
  }
}

export function categoryLabel(category: string): string {
  switch (category) {
    case 'WATER_ELECTRICITY':
      return '水电';
    case 'CIVIL_WATERPROOF':
      return '土建防水';
    case 'DOOR_WINDOW':
      return '门窗五金';
    case 'HVAC':
      return '暖通空调';
    case 'ELEVATOR':
      return '电梯设备';
    case 'PUBLIC_FACILITY':
      return '公共设施';
    default:
      return '其他';
  }
}

/** 流转动作的中文标签（docs/06 order_flow_record.action 口径）。 */
export function actionLabel(action: string): string {
  switch (action) {
    case 'SUBMIT':
      return '提交报修';
    case 'ACCEPT':
      return '客服受理';
    case 'DISPATCH':
      return '派单';
    case 'REASSIGN':
      return '改派';
    case 'REJECT':
      return '拒单';
    case 'START':
      return '师傅到场';
    case 'PROCESSING':
      return '维修中';
    case 'COMPLETE':
      return '完工提交';
    case 'CONFIRM':
      return '验收通过';
    case 'REJECT_CONFIRM':
      return '验收不通过';
    case 'URGE':
      return '催办';
    case 'EXTERNAL_FOLLOWUP':
      return '物业跟进';
    case 'CLOSE':
      return '关单';
    case 'CANCEL':
      return '取消';
    default:
      return action;
  }
}

/** 保修判定结论的通俗话术（业主端展示用，docs/03 §4）。 */
export function verdictText(verdict: string): string {
  switch (verdict) {
    case 'IN_WARRANTY':
      return '保修期内，将由开发商负责维修';
    case 'OUT_OF_WARRANTY':
      return '已过保修期，由物业跟进处理';
    case 'OWNER_RESPONSIBLE':
      return '属人为损坏，需有偿维修';
    default:
      return '判定中';
  }
}
