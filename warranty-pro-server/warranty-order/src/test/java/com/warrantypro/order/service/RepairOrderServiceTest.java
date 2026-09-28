package com.warrantypro.order.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrantypro.common.enums.OrderStatus;
import com.warrantypro.common.exception.BizException;
import com.warrantypro.common.security.LoginUser;
import com.warrantypro.estate.mapper.BuildingMapper;
import com.warrantypro.estate.mapper.FacilityMapper;
import com.warrantypro.estate.mapper.HouseMapper;
import com.warrantypro.order.entity.OrderFlowRecord;
import com.warrantypro.order.entity.RepairOrder;
import com.warrantypro.order.mapper.EvaluationMapper;
import com.warrantypro.order.mapper.OrderFlowRecordMapper;
import com.warrantypro.order.mapper.RepairOrderMapper;
import com.warrantypro.order.mapper.RepairReportMapper;
import com.warrantypro.user.mapper.UserHouseMapper;
import com.warrantypro.warranty.service.WarrantyVerdictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairOrderServiceTest {

    @Mock private RepairOrderMapper repairOrderMapper;
    @Mock private EvaluationMapper evaluationMapper;
    @Mock private OrderFlowRecordMapper orderFlowRecordMapper;
    @Mock private RepairReportMapper repairReportMapper;
    @Mock private UserHouseMapper userHouseMapper;
    @Mock private HouseMapper houseMapper;
    @Mock private BuildingMapper buildingMapper;
    @Mock private FacilityMapper facilityMapper;
    @Mock private WarrantyVerdictService warrantyVerdictService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private RepairOrderService service;

    @BeforeEach
    void setUp() {
        service = new RepairOrderService(repairOrderMapper, evaluationMapper, orderFlowRecordMapper,
                repairReportMapper, userHouseMapper, houseMapper, buildingMapper, facilityMapper,
                warrantyVerdictService, eventPublisher, new ObjectMapper());
    }

    @Test
    void confirmPassCompletesOrderAndUpdatesWorkerCompletionCount() {
        RepairOrder order = pendingConfirmation();
        when(repairOrderMapper.selectById(7L)).thenReturn(order);
        when(repairOrderMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        var result = service.confirm(owner(), 7L, true, null);

        assertEquals(OrderStatus.COMPLETED.name(), result.status());
        assertNotNull(order.getConfirmedAt());
        verify(orderFlowRecordMapper).insert(argThat((OrderFlowRecord flow) ->
                OrderStatus.PENDING_CONFIRM.name().equals(flow.getFromStatus())
                        && OrderStatus.COMPLETED.name().equals(flow.getToStatus())));
        verify(evaluationMapper).incrementCompleted(22L);
    }

    @Test
    void rejectConfirmationReturnsOrderToInProgress() {
        RepairOrder order = pendingConfirmation();
        when(repairOrderMapper.selectById(7L)).thenReturn(order);
        when(repairOrderMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        var result = service.confirm(owner(), 7L, false, "仍然漏水");

        assertEquals(OrderStatus.IN_PROGRESS.name(), result.status());
        verify(orderFlowRecordMapper).insert(argThat((OrderFlowRecord flow) ->
                OrderStatus.IN_PROGRESS.name().equals(flow.getToStatus())
                        && "仍然漏水".equals(flow.getRemark())));
        verify(evaluationMapper, never()).incrementCompleted(anyLong());
    }

    @Test
    void expiredConfirmationUsesConditionalUpdateAndWritesCompletionOnce() {
        RepairOrder order = pendingConfirmation();
        LocalDateTime deadline = LocalDateTime.now().minusHours(48);
        order.setCompletedAt(deadline.minusMinutes(1));
        when(repairOrderMapper.selectById(7L)).thenReturn(order);
        when(repairOrderMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        assertTrue(service.autoConfirmExpired(7L, deadline));

        ArgumentCaptor<OrderFlowRecord> flow = ArgumentCaptor.forClass(OrderFlowRecord.class);
        verify(orderFlowRecordMapper).insert(flow.capture());
        assertEquals(OrderStatus.PENDING_CONFIRM.name(), flow.getValue().getFromStatus());
        assertEquals(OrderStatus.COMPLETED.name(), flow.getValue().getToStatus());
        assertTrue(flow.getValue().getRemark().contains("自动验收"));
        verify(evaluationMapper).incrementCompleted(22L);
    }

    @Test
    void expiredConfirmationDoesNothingAfterConcurrentOwnerConfirmation() {
        RepairOrder order = pendingConfirmation();
        LocalDateTime deadline = LocalDateTime.now().minusHours(48);
        order.setCompletedAt(deadline.minusMinutes(1));
        when(repairOrderMapper.selectById(7L)).thenReturn(order);
        when(repairOrderMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(0);

        assertFalse(service.autoConfirmExpired(7L, deadline));

        verify(orderFlowRecordMapper, never()).insert(any(OrderFlowRecord.class));
        verify(evaluationMapper, never()).incrementCompleted(anyLong());
    }

    @Test
    void ownerCannotConfirmAnotherOwnersOrder() {
        RepairOrder order = pendingConfirmation();
        order.setOwnerId(99L);
        when(repairOrderMapper.selectById(7L)).thenReturn(order);

        assertThrows(BizException.class, () -> service.confirm(owner(), 7L, true, null));
        verify(repairOrderMapper, never()).updateById(any(RepairOrder.class));
    }

    private static RepairOrder pendingConfirmation() {
        RepairOrder order = new RepairOrder();
        order.setId(7L);
        order.setOwnerId(11L);
        order.setCurrentWorkerId(22L);
        order.setStatus(OrderStatus.PENDING_CONFIRM.name());
        order.setCompletedAt(LocalDateTime.now().minusHours(1));
        return order;
    }

    private static LoginUser owner() {
        return new LoginUser(11L, "13800000000", "业主", List.of("OWNER"));
    }
}
