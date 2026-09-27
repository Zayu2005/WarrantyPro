package com.warrantypro.common.enums;

/**
 * 工单状态机（docs/03 §1，全文档统一口径；DB 存枚举名，见 docs/06 §1）。
 */
public enum OrderStatus {

    SUBMITTED("待受理"),
    PENDING_DISPATCH("待派单"),
    EXTERNAL_PROCESSING("外部处理中（保修期内）"),
    DISPATCHED("已派单·待上门"),
    IN_PROGRESS("维修中"),
    PENDING_CONFIRM("待验收"),
    COMPLETED("已完结"),
    CANCELLED("已取消");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 业主是否可撤销（仅待受理） */
    public boolean canCancelByOwner() {
        return this == SUBMITTED;
    }

    /** 是否处于派单流转环节（供派单服务校验） */
    public boolean isDispatchable() {
        return this == PENDING_DISPATCH;
    }
}
