package com.warrantypro.common.enums;

/**
 * 派单模式（docs/05 §4.3）：推荐确认 / 全自动 / 人工指定。
 */
public enum DispatchMode {

    RECOMMEND("推荐确认（默认）"),
    AUTO("全自动"),
    MANUAL("人工指定");

    private final String label;

    DispatchMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
