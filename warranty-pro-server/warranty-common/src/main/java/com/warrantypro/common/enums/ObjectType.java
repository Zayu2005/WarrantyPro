package com.warrantypro.common.enums;

/**
 * 报修对象（docs/06 §3.3 repair_order.object_type）。
 */
public enum ObjectType {

    INDOOR("户内部位"),
    PUBLIC_FACILITY("公共设施");

    private final String label;

    ObjectType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
