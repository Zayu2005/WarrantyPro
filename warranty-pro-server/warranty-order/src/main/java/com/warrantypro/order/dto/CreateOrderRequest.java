package com.warrantypro.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 表单报修请求（docs/07 §3.1；对话式报修经小保生成草稿后走同一接口，source = AI_CHAT）。
 */
public record CreateOrderRequest(
        /** INDOOR / PUBLIC_FACILITY */
        @NotBlank
        String objectType,

        /** 故障类别代码（FaultCategory） */
        @NotBlank
        String category,

        /** 户内报修必填 */
        Long houseId,

        /** 公共设施报修必填 */
        Long facilityId,

        @NotBlank
        @Size(max = 100)
        String locationDetail,

        @NotBlank
        @Size(max = 500)
        String phenomenon,

        /** URGENT / NORMAL，缺省 NORMAL */
        String urgency,

        /** 小保会话 ID（AI 报修时用于追溯，可空） */
        Long sourceSessionId
) {
}
