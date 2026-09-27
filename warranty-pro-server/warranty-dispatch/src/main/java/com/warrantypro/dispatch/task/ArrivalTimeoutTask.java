package com.warrantypro.dispatch.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warrantypro.common.enums.OrderStatus;
import com.warrantypro.dispatch.service.DispatchService;
import com.warrantypro.order.entity.RepairOrder;
import com.warrantypro.order.mapper.RepairOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 到场超时扫描：已派单但师傅未在截止时间前到场打卡的工单，自动改派次优候选（docs/03 §5.2）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArrivalTimeoutTask {

    private final RepairOrderMapper repairOrderMapper;
    private final DispatchService dispatchService;

    @Scheduled(fixedDelayString = "${warranty.dispatch.arrive-scan-ms:60000}")
    public void scan() {
        List<RepairOrder> overdue = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getStatus, OrderStatus.DISPATCHED.name())
                .lt(RepairOrder::getAcceptDeadline, LocalDateTime.now()));
        for (RepairOrder order : overdue) {
            log.info("工单 {} 到场超时，触发自动改派", order.getOrderNo());
            try {
                dispatchService.reassignOnTimeout(order.getId());
            } catch (Exception e) {
                log.error("超时改派失败，orderId={}", order.getId(), e);
            }
        }
    }
}
