package com.warrantypro.common.enums;

/**
 * 保修到期预警对象类型（docs/06 warranty_alert.alert_target，G6 优化项）。
 */
public enum AlertTarget {

    FACILITY("设施设备"),
    BUILDING("楼栋（户内保修到期）");

    private final String label;

    AlertTarget(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
