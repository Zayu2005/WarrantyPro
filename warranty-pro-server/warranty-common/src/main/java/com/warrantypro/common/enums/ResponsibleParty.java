package com.warrantypro.common.enums;

/**
 * 保修责任方（docs/03 §4 responsibleParty 字段口径）。
 */
public enum ResponsibleParty {

    DEVELOPER("开发商 / 承建商"),
    PROPERTY("物业公司"),
    OWNER("业主");

    private final String label;

    ResponsibleParty(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
