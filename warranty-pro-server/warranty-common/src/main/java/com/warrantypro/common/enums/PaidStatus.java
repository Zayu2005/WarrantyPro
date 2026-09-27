package com.warrantypro.common.enums;

/**
 * 有偿维修收费状态（docs/06 repair_order.paid_status，G3 优化项）。
 */
public enum PaidStatus {

    NOT_REQUIRED("无需收费"),
    UNPAID("待收费"),
    PAID("已收费");

    private final String label;

    PaidStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
