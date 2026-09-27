package com.warrantypro.common.enums;

/**
 * Agent 类型（docs/05 §1 职责边界表；DB 取值见 docs/06 §3.4）。
 */
public enum AgentType {

    XIAOBAO("小保·对话式报修助手"),
    DISPATCH("智能派单（可解释决策）"),
    REPAIR_ASSIST("维修辅助"),
    ANALYSIS("运营分析");

    private final String label;

    AgentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
