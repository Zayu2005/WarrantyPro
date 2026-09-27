package com.warrantypro.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warrantypro.common.enums.FaultCategory;
import com.warrantypro.common.enums.ObjectType;
import com.warrantypro.common.enums.OrderAction;
import com.warrantypro.common.enums.OrderSource;
import com.warrantypro.common.enums.OrderStatus;
import com.warrantypro.common.enums.RoleCode;
import com.warrantypro.common.enums.Urgency;
import com.warrantypro.common.enums.Verdict;
import com.warrantypro.common.event.OrderAcceptedEvent;
import com.warrantypro.common.exception.BizException;
import com.warrantypro.common.exception.ErrorCode;
import com.warrantypro.common.result.PageResult;
import com.warrantypro.common.security.LoginUser;
import com.warrantypro.estate.entity.Building;
import com.warrantypro.estate.entity.Facility;
import com.warrantypro.estate.entity.House;
import com.warrantypro.estate.mapper.BuildingMapper;
import com.warrantypro.estate.mapper.FacilityMapper;
import com.warrantypro.estate.mapper.HouseMapper;
import com.warrantypro.order.dto.CreateOrderRequest;
import com.warrantypro.order.dto.OrderCreateResponse;
import com.warrantypro.order.dto.OrderVO;
import com.warrantypro.order.entity.OrderFlowRecord;
import com.warrantypro.order.entity.RepairOrder;
import com.warrantypro.order.entity.RepairReport;
import com.warrantypro.order.mapper.OrderFlowRecordMapper;
import com.warrantypro.order.mapper.RepairOrderMapper;
import com.warrantypro.order.mapper.RepairReportMapper;
import com.warrantypro.user.entity.UserHouse;
import com.warrantypro.user.mapper.UserHouseMapper;
import com.warrantypro.warranty.dto.VerdictResult;
import com.warrantypro.warranty.service.WarrantyVerdictService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报修工单服务：提交（含保修判定快照）、业主查询、客服工单池与受理（docs/03 §2、§4）。
 */
@Service
@RequiredArgsConstructor
public class RepairOrderService {

    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RepairOrderMapper repairOrderMapper;
    private final OrderFlowRecordMapper orderFlowRecordMapper;
    private final RepairReportMapper repairReportMapper;
    private final UserHouseMapper userHouseMapper;
    private final HouseMapper houseMapper;
    private final BuildingMapper buildingMapper;
    private final FacilityMapper facilityMapper;
    private final WarrantyVerdictService warrantyVerdictService;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== 业主端 ====================

    @Transactional
    public OrderCreateResponse create(LoginUser owner, CreateOrderRequest req) {
        ObjectType objectType = parseEnum(ObjectType.class, req.objectType(), "报修对象");
        FaultCategory category = parseEnum(FaultCategory.class, req.category(), "故障类别");
        Urgency urgency = req.urgency() == null ? Urgency.NORMAL : parseEnum(Urgency.class, req.urgency(), "紧急程度");

        Long communityId;
        if (objectType == ObjectType.INDOOR) {
            if (req.houseId() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "户内报修必须指定房屋");
            }
            UserHouse binding = userHouseMapper.selectOne(new LambdaQueryWrapper<UserHouse>()
                    .eq(UserHouse::getUserId, owner.userId())
                    .eq(UserHouse::getHouseId, req.houseId())
                    .eq(UserHouse::getStatus, "APPROVED"));
            if (binding == null) {
                throw new BizException(ErrorCode.FORBIDDEN, "未绑定该房屋或绑定未通过审核");
            }
            communityId = resolveCommunityId(req.houseId(), null);
        } else {
            if (req.facilityId() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "公共设施报修必须指定设施");
            }
            Facility facility = facilityMapper.selectById(req.facilityId());
            if (facility == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "报修设施不存在");
            }
            communityId = facility.getCommunityId();
        }

        // 保修判定（预判定快照，客服受理时可复核纠正）
        VerdictResult verdict = warrantyVerdictService.judge(objectType, category, req.houseId(), req.facilityId());

        RepairOrder order = new RepairOrder();
        order.setOrderNo(nextOrderNo());
        order.setCommunityId(communityId);
        order.setHouseId(req.houseId());
        order.setFacilityId(req.facilityId());
        order.setOwnerId(owner.userId());
        order.setObjectType(objectType.name());
        order.setCategory(category.name());
        order.setLocationDetail(req.locationDetail());
        order.setPhenomenon(req.phenomenon());
        order.setUrgency(urgency.name());
        order.setSource(OrderSource.FORM.name());
        order.setSourceSessionId(req.sourceSessionId());
        order.setStatus(OrderStatus.SUBMITTED.name());
        order.setVerdict(verdict.verdict().name());
        order.setResponsibleParty(verdict.responsibleParty().name());
        order.setVerdictBasis(verdict.basis());
        order.setWarrantyStart(verdict.warrantyStart());
        order.setWarrantyExpire(verdict.warrantyExpire());
        order.setPaidStatus("NOT_REQUIRED");
        order.setSubmittedAt(LocalDateTime.now());
        repairOrderMapper.insert(order);

        recordFlow(order.getId(), null, OrderStatus.SUBMITTED, owner.userId(), RoleCode.OWNER.name(),
                OrderAction.SUBMIT, null);
        return new OrderCreateResponse(order.getId(), order.getOrderNo(), order.getStatus(), verdict);
    }

    public PageResult<OrderVO> myOrders(LoginUser owner, String status, long page, long pageSize) {
        Page<RepairOrder> result = repairOrderMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<RepairOrder>()
                        .eq(RepairOrder::getOwnerId, owner.userId())
                        .eq(status != null && !status.isBlank(), RepairOrder::getStatus, status)
                        .orderByDesc(RepairOrder::getId));
        return PageResult.of(result.getRecords().stream().map(o -> toVO(o, false)).toList(),
                result.getTotal(), page, pageSize);
    }

    public OrderVO detail(LoginUser user, Long orderId) {
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        boolean isOwner = order.getOwnerId().equals(user.userId());
        boolean isStaff = user.roles().stream().anyMatch(r ->
                RoleCode.DISPATCHER.name().equals(r) || RoleCode.MANAGER.name().equals(r)
                        || RoleCode.ADMIN.name().equals(r) || RoleCode.WORKER.name().equals(r));
        if (!isOwner && !isStaff) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该工单");
        }
        return toVO(order, true);
    }

    // ==================== 师傅端 ====================

    /** 师傅工单：默认待办（待接单 + 维修中）；history=true 时含已完结（订单 Tab 历史）。 */
    public PageResult<OrderVO> workerOrders(LoginUser worker, boolean history, long page, long pageSize) {
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getCurrentWorkerId, worker.userId())
                .orderByDesc(RepairOrder::getId);
        if (history) {
            wrapper.in(RepairOrder::getStatus,
                    OrderStatus.DISPATCHED.name(), OrderStatus.IN_PROGRESS.name(), OrderStatus.COMPLETED.name());
        } else {
            wrapper.in(RepairOrder::getStatus,
                    OrderStatus.DISPATCHED.name(), OrderStatus.IN_PROGRESS.name());
        }
        Page<RepairOrder> result = repairOrderMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords().stream().map(o -> toVO(o, false)).toList(),
                result.getTotal(), page, pageSize);
    }

    // ==================== 客服调度端 ====================

    public PageResult<OrderVO> dispatchPool(String status, String category, Long communityId,
                                            long page, long pageSize) {        Page<RepairOrder> result = repairOrderMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<RepairOrder>()
                        .eq(status != null && !status.isBlank(), RepairOrder::getStatus, status)
                        .eq(category != null && !category.isBlank(), RepairOrder::getCategory, category)
                        .eq(communityId != null, RepairOrder::getCommunityId, communityId)
                        .orderByAsc(RepairOrder::getId));
        return PageResult.of(result.getRecords().stream().map(o -> toVO(o, false)).toList(),
                result.getTotal(), page, pageSize);
    }

    /**
     * 受理：按判定快照路由（保修期内 → 外部处理中；期外 → 待派单），随后发布受理事件
     * 触发智能体直派（同步监听，事件处理完成后重读工单，响应即含派单结果）。
     */
    @Transactional
    public OrderVO accept(LoginUser dispatcher, Long orderId) {
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        if (!OrderStatus.SUBMITTED.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仅待受理状态的工单可以受理");
        }
        OrderStatus target = Verdict.IN_WARRANTY.name().equals(order.getVerdict())
                ? OrderStatus.EXTERNAL_PROCESSING
                : OrderStatus.PENDING_DISPATCH;
        order.setStatus(target.name());
        order.setAcceptedAt(LocalDateTime.now());
        repairOrderMapper.updateById(order);

        recordFlow(order.getId(), OrderStatus.SUBMITTED, target, dispatcher.userId(),
                RoleCode.DISPATCHER.name(), OrderAction.ACCEPT, null);

        if (target == OrderStatus.PENDING_DISPATCH) {
            eventPublisher.publishEvent(new OrderAcceptedEvent(order.getId()));
        }
        return toVO(repairOrderMapper.selectById(orderId), false);
    }

    // ==================== 师傅端（迭代 2：无接单环节） ====================

    /** 到场打卡：已派单·待上门 → 维修中。 */
    @Transactional
    public OrderVO arrive(LoginUser worker, Long orderId) {
        RepairOrder order = requireWorkerOrder(worker, orderId);
        order.setStatus(OrderStatus.IN_PROGRESS.name());
        order.setStartedAt(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        recordFlow(order.getId(), OrderStatus.DISPATCHED, OrderStatus.IN_PROGRESS,
                worker.userId(), RoleCode.WORKER.name(), OrderAction.START, null);
        return toVO(order, false);
    }

    /** 完工提交：维修记录入库 → 待验收（docs/02 FR-W-06/08）。 */
    @Transactional
    public OrderVO complete(LoginUser worker, Long orderId,
                            String faultCause, String measures, BigDecimal workHours) {
        RepairOrder order = requireWorkerOrder(worker, orderId);
        if (!OrderStatus.IN_PROGRESS.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仅维修中的工单可以完工提交");
        }
        RepairReport report = repairReportMapper.selectOne(new LambdaQueryWrapper<RepairReport>()
                .eq(RepairReport::getOrderId, orderId));
        if (report == null) {
            report = new RepairReport();
            report.setOrderId(orderId);
        }
        report.setWorkerId(worker.userId());
        report.setFaultCause(faultCause == null ? "" : faultCause);
        report.setMeasures(measures == null ? "" : measures);
        report.setWorkHours(workHours);
        report.setAiAssisted(0);
        if (report.getId() == null) {
            repairReportMapper.insert(report);
        } else {
            repairReportMapper.updateById(report);
        }

        order.setStatus(OrderStatus.PENDING_CONFIRM.name());
        order.setCompletedAt(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        recordFlow(order.getId(), OrderStatus.IN_PROGRESS, OrderStatus.PENDING_CONFIRM,
                worker.userId(), RoleCode.WORKER.name(), OrderAction.COMPLETE, null);
        return toVO(order, false);
    }

    private RepairOrder requireWorkerOrder(LoginUser worker, Long orderId) {
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        if (!worker.userId().equals(order.getCurrentWorkerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "该工单未派给您");
        }
        return order;
    }

    /** 业主验收：通过 → 已完结；不通过 → 退回维修中（返工），docs/03 §6。 */
    @Transactional
    public OrderVO confirm(LoginUser owner, Long orderId, boolean pass, String reason) {
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        if (!owner.userId().equals(order.getOwnerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅报修人可以验收");
        }
        if (!OrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仅待验收状态的工单可以验收");
        }
        OrderStatus target = pass ? OrderStatus.COMPLETED : OrderStatus.IN_PROGRESS;
        order.setStatus(target.name());
        order.setConfirmedAt(LocalDateTime.now());
        repairOrderMapper.updateById(order);

        recordFlow(order.getId(), OrderStatus.PENDING_CONFIRM, target,
                owner.userId(), RoleCode.OWNER.name(),
                pass ? OrderAction.CONFIRM : OrderAction.REJECT_CONFIRM,
                pass ? null : reason);
        return toVO(order, false);
    }

    // ==================== 内部方法 ====================

    private Long resolveCommunityId(Long houseId, Long facilityId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "报修房屋不存在");
        }
        Building building = buildingMapper.selectById(house.getBuildingId());
        if (building == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "房屋所属楼栋不存在");
        }
        return building.getCommunityId();
    }

    /** 工单号：WO + yyyyMMdd + 当日序号（JVM 内同步保证唯一；多实例部署需换 Redis 自增）。 */
    private synchronized String nextOrderNo() {
        long todayCount = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .ge(RepairOrder::getCreatedAt, LocalDate.now().atStartOfDay()));
        return "WO" + LocalDate.now().format(ORDER_NO_DATE) + String.format("%04d", todayCount + 1);
    }

    private void recordFlow(Long orderId, OrderStatus from, OrderStatus to, Long operatorId,
                            String operatorRole, OrderAction action, String remark) {
        OrderFlowRecord flow = new OrderFlowRecord();
        flow.setOrderId(orderId);
        flow.setFromStatus(from == null ? null : from.name());
        flow.setToStatus(to.name());
        flow.setOperatorId(operatorId);
        flow.setOperatorRole(operatorRole);
        flow.setAction(action.name());
        flow.setRemark(remark);
        orderFlowRecordMapper.insert(flow);
    }

    private OrderVO toVO(RepairOrder o, boolean withFlows) {
        List<OrderVO.FlowVO> flows = null;
        if (withFlows) {
            flows = orderFlowRecordMapper.selectList(new LambdaQueryWrapper<OrderFlowRecord>()
                            .eq(OrderFlowRecord::getOrderId, o.getId())
                            .orderByAsc(OrderFlowRecord::getId))
                    .stream().map(f -> new OrderVO.FlowVO(f.getId(), f.getFromStatus(), f.getToStatus(),
                            f.getOperatorId(), f.getOperatorRole(), f.getAction(), f.getRemark(), f.getCreatedAt()))
                    .toList();
        }
        return new OrderVO(o.getId(), o.getOrderNo(), o.getCommunityId(), o.getHouseId(), o.getFacilityId(),
                o.getOwnerId(), o.getObjectType(), o.getCategory(), o.getLocationDetail(), o.getPhenomenon(),
                o.getUrgency(), o.getSource(), o.getStatus(), o.getVerdict(), o.getResponsibleParty(),
                o.getVerdictBasis(), o.getWarrantyStart(), o.getWarrantyExpire(), o.getPaidStatus(),
                o.getFeeAmount(), o.getCurrentWorkerId(), o.getSubmittedAt(), o.getAcceptedAt(),
                o.getCompletedAt(), o.getConfirmedAt(), o.getCreatedAt(), flows);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String label) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "非法的" + label + "：" + value);
        }
    }
}
