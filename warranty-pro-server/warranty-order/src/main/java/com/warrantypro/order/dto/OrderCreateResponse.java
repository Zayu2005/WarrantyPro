package com.warrantypro.order.dto;

import com.warrantypro.warranty.dto.VerdictResult;

/** 提交工单响应：工单标识；verdict 字段保留用于兼容旧客户端，新工单为空。 */
public record OrderCreateResponse(
        Long orderId,
        String orderNo,
        String status,
        VerdictResult verdict
) {
}
