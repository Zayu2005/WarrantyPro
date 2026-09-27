package com.warrantypro.common.enums;

/**
 * 工单流转动作（docs/06 §3.3 order_flow_record.action 的取值口径）。
 */
public enum OrderAction {

    SUBMIT("提交报修"),
    ACCEPT("受理"),
    DISPATCH("派单"),
    REASSIGN("改派"),
    REJECT("拒单 / 申请改派"),
    START("到场 / 开始维修"),
    PROCESSING("处理中打卡"),
    COMPLETE("完工提交"),
    CONFIRM("验收通过"),
    REJECT_CONFIRM("验收不通过（返工）"),
    URGE("催办"),
    CLOSE("关单"),
    CANCEL("取消");

    private final String label;

    OrderAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
