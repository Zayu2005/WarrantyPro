package com.warrantypro.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RepairWorkflowRequest(
        @NotBlank(message = "故障描述不能为空")
        @Size(max = 2000, message = "故障描述不能超过 2000 字")
        String phenomenon,
        @Size(max = 60, message = "故障类别过长")
        String category,
        @Size(max = 200, message = "位置描述不能超过 200 字")
        String locationDetail,
        @Size(max = 20, message = "紧急程度过长")
        String urgency
) {
}
