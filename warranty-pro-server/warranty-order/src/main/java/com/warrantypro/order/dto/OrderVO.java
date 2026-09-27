package com.warrantypro.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 工单 VO（列表与详情共用；详情额外携带流转时间轴）。 */
public record OrderVO(
        Long id,
        String orderNo,
        Long communityId,
        Long houseId,
        Long facilityId,
        Long ownerId,
        String objectType,
        String category,
        String locationDetail,
        String phenomenon,
        String urgency,
        String source,
        String status,
        String verdict,
        String responsibleParty,
        String verdictBasis,
        LocalDate warrantyStart,
        LocalDate warrantyExpire,
        String paidStatus,
        BigDecimal feeAmount,
        Long currentWorkerId,
        LocalDateTime submittedAt,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt,
        List<FlowVO> flows
) {

    public record FlowVO(
            Long id,
            String fromStatus,
            String toStatus,
            Long operatorId,
            String operatorRole,
            String action,
            String remark,
            LocalDateTime createdAt
    ) {
    }
}
