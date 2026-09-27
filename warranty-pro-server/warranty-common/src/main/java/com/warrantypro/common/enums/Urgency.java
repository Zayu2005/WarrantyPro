package com.warrantypro.common.enums;

/**
 * 紧急程度（docs/03 §8 影响受理与到场超时阈值）。
 */
public enum Urgency {

    URGENT("紧急"),
    NORMAL("普通");

    private final String label;

    Urgency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
