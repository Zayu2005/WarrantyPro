package com.warrantypro.order.dto;

import com.warrantypro.warranty.dto.VerdictResult;

/**
 * 提交工单响应：工单标识 + 保修判定书（业主端展示通俗结论，判定书随工单快照留存）。
 */
public record OrderCreateResponse(
        Long orderId,
        String orderNo,
        String status,
        VerdictResult verdict
) {
}
