package com.warrantypro.common.enums;

/**
 * 报修来源（docs/06 §3.3 repair_order.source）：AI 对话式 / 传统表单（降级方式，docs/05 §1）。
 */
public enum OrderSource {

    AI_CHAT("对话式智能报修"),
    FORM("表单报修");

    private final String label;

    OrderSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
