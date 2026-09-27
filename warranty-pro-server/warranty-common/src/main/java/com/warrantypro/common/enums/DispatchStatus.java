package com.warrantypro.common.enums;

/**
 * 派单记录状态（docs/06 dispatch_record.status，迭代 2 口径：无接单环节）。
 */
public enum DispatchStatus {

    DISPATCHED("已派单（生效中）"),
    ARRIVED("师傅已到场"),
    SUPERSEDED("被改派替代"),
    TIMEOUT_REASSIGNED("到场超时自动改派");

    private final String label;

    DispatchStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
