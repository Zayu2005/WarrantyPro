package com.warrantypro.common.enums;

/**
 * 系统角色（docs/02 §1，全文档统一口径）。
 */
public enum RoleCode {

    OWNER("业主 / 住户"),
    WORKER("维修师傅"),
    DISPATCHER("物业客服（调度员）"),
    MANAGER("物业管理层"),
    ADMIN("系统管理员");

    private final String label;

    RoleCode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
