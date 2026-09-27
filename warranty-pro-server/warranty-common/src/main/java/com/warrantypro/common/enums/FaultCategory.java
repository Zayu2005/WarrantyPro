package com.warrantypro.common.enums;

/**
 * 故障类别（docs/README 术语表口径；师傅技能标签 skill_tags 与其对齐，见 docs/06 §3.3 worker_profile）。
 */
public enum FaultCategory {

    WATER_ELECTRICITY("水电"),
    CIVIL_WATERPROOF("土建防水"),
    DOOR_WINDOW("门窗五金"),
    HVAC("暖通空调"),
    ELEVATOR("电梯设备"),
    PUBLIC_FACILITY("公共设施"),
    OTHER("其他");

    private final String label;

    FaultCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
