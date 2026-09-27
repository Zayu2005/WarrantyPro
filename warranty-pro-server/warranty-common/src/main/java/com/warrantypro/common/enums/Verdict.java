package com.warrantypro.common.enums;

/**
 * 保修判定结果（docs/03 §4 判定书 verdict 字段口径，工单表冗余快照 docs/06 §3.3）。
 */
public enum Verdict {

    IN_WARRANTY("保修期内"),
    OUT_OF_WARRANTY("保修期外"),
    OWNER_RESPONSIBLE("人为损坏（业主责任）");

    private final String label;

    Verdict(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
