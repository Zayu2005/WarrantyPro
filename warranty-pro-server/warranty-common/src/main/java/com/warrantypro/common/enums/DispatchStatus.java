package com.warrantypro.common.enums;

/**
 * 派单记录状态（docs/06 dispatch_record.status，G1 优化项）。
 */
public enum DispatchStatus {

    DISPATCHED("待接单"),
    ACCEPTED("已接单"),
    REJECTED("待接期拒单（直接回流派单池）"),
    PENDING_REASSIGN("维修中申请改派（待客服审批）"),
    TIMEOUT_REASSIGNED("接单超时自动改派");

    private final String label;

    DispatchStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
