package com.warrantypro.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warrantypro.common.enums.OrderStatus;
import com.warrantypro.order.entity.RepairOrder;
import com.warrantypro.order.mapper.RepairOrderMapper;
import com.warrantypro.order.service.RepairOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** 超过配置验收时限仍未处理的工单，默认验收通过。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfirmationTimeoutTask {

    @Value("${warranty.confirm.auto-pass-hours:48}")
    private int autoPassHours;

    private final RepairOrderMapper repairOrderMapper;
    private final RepairOrderService repairOrderService;

    @Scheduled(fixedDelayString = "${warranty.confirm.scan-ms:60000}")
    public void scan() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(Math.max(1, autoPassHours));
        List<RepairOrder> overdue = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getStatus, OrderStatus.PENDING_CONFIRM.name())
                .le(RepairOrder::getCompletedAt, deadline));

        for (RepairOrder order : overdue) {
            try {
                if (repairOrderService.autoConfirmExpired(order.getId(), deadline)) {
                    log.info("工单 {} 验收超时，系统默认通过", order.getOrderNo());
                }
            } catch (Exception e) {
                log.error("验收超时自动通过失败，orderId={}", order.getId(), e);
            }
        }
    }
}
