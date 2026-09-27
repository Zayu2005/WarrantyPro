package com.warrantypro.dispatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrantypro.common.event.OrderAcceptedEvent;
import com.warrantypro.common.enums.FaultCategory;
import com.warrantypro.common.enums.OrderAction;
import com.warrantypro.common.enums.OrderStatus;
import com.warrantypro.common.exception.BizException;
import com.warrantypro.common.exception.ErrorCode;
import com.warrantypro.dispatch.config.DispatchProperties;
import com.warrantypro.dispatch.entity.AgentDecisionLog;
import com.warrantypro.dispatch.entity.CandidateWorker;
import com.warrantypro.dispatch.entity.DispatchRecord;
import com.warrantypro.dispatch.mapper.AgentDecisionLogMapper;
import com.warrantypro.dispatch.mapper.DispatchRecordMapper;
import com.warrantypro.dispatch.mapper.WorkerProfileMapper;
import com.warrantypro.dispatch.mapper.WorkerScheduleMapper;
import com.warrantypro.order.entity.OrderFlowRecord;
import com.warrantypro.order.entity.RepairOrder;
import com.warrantypro.order.mapper.OrderFlowRecordMapper;
import com.warrantypro.order.mapper.RepairOrderMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能派单服务（docs/03 §5、docs/05 §4）。
 *
 * <p>规则引擎确定性评分：候选过滤（在岗 ∧ 当日排班 ∧ 负载上限，技能不限领域）
 * → 四因子加权（技能 0.4 仅作偏好 / 负载 0.25 / 位置 0.2 / 评分 0.15，权重可配）→ Top1 直派。
 * 派单记录与决策日志全量落库，可解释可审计；LLM 理由生成在接入模型 API 后叠加（迭代 3）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final RepairOrderMapper repairOrderMapper;
    private final OrderFlowRecordMapper orderFlowRecordMapper;
    private final DispatchRecordMapper dispatchRecordMapper;
    private final WorkerProfileMapper workerProfileMapper;
    private final WorkerScheduleMapper workerScheduleMapper;
    private final AgentDecisionLogMapper agentDecisionLogMapper;
    private final DispatchProperties properties;
    private final ObjectMapper objectMapper;

    /** 客服受理后同步直派（同一事务，失败不阻断受理）。 */
    @EventListener
    public void onOrderAccepted(OrderAcceptedEvent event) {
        try {
            tryDispatch(event.getOrderId(), 1, null, "客服受理后智能直派");
        } catch (Exception e) {
            log.error("受理直派失败，orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 尝试为待派单工单直派 Top1 候选。无候选 / 非待派单状态时静默返回（工单停留待派单，由客服人工处理）。
     */
    @Transactional
    public void tryDispatch(Long orderId, int round, Long excludeWorkerId, String trigger) {
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null || !OrderStatus.PENDING_DISPATCH.name().equals(order.getStatus())) {
            return;
        }
        long start = System.currentTimeMillis();
        FaultCategory category = parseCategory(order.getCategory());

        // 1. 候选池：在岗 ∧ 当日排班
        List<CandidateWorker> pool = workerProfileMapper.selectDutyWorkers(LocalDate.now());

        // 2. 负载过滤 + 评分（技能不限领域：任何在班师傅均可接单，技能仅作加分偏好）
        List<Scored> candidates = new ArrayList<>();
        for (CandidateWorker w : pool) {
            if (excludeWorkerId != null && w.getUserId().equals(excludeWorkerId)) {
                continue;
            }
            long inProgress = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                    .eq(RepairOrder::getCurrentWorkerId, w.getUserId())
                    .in(RepairOrder::getStatus,
                            OrderStatus.DISPATCHED.name(), OrderStatus.IN_PROGRESS.name()));
            int max = w.getMaxConcurrent() == null ? properties.getMaxConcurrent() : w.getMaxConcurrent();
            if (inProgress >= max) {
                continue;
            }
            candidates.add(score(w, inProgress, max, category, order));
        }
        candidates.sort((a, b) -> Double.compare(b.score, a.score));

        if (candidates.isEmpty()) {
            logDecision(order, round, trigger, List.of(), null, "当前无在班候选师傅，等待客服人工派单", "DEGRADED", start);
            return;
        }

        // 3. Top1 直派
        Scored best = candidates.get(0);
        order.setStatus(OrderStatus.DISPATCHED.name());
        order.setCurrentWorkerId(best.worker.getUserId());
        order.setDispatchMode("AUTO");
        order.setAcceptDeadline(LocalDateTime.now().plusHours(properties.getArriveTimeoutHours()));
        repairOrderMapper.updateById(order);

        DispatchRecord record = new DispatchRecord();
        record.setOrderId(order.getId());
        record.setWorkerId(best.worker.getUserId());
        record.setMode("AUTO");
        record.setScore(BigDecimal.valueOf(best.score).setScale(3, RoundingMode.HALF_UP));
        record.setFactors(toJson(best.factors));
        record.setReason(buildReason(best));
        record.setStatus("DISPATCHED");
        record.setRoundNo(round);
        record.setRejectReason(trigger);
        dispatchRecordMapper.insert(record);

        recordFlow(order.getId(), OrderStatus.PENDING_DISPATCH, OrderStatus.DISPATCHED, OrderAction.DISPATCH, record.getReason());

        List<Map<String, Object>> candidateSummary = candidates.stream()
                .limit(3).map(c -> Map.<String, Object>of(
                        "workerId", c.worker.getUserId(),
                        "name", c.worker.getRealName(),
                        "score", round3(c.score)))
                .toList();
        logDecision(order, round, trigger, candidateSummary, best.worker.getUserId(),
                record.getReason(), "OK", start);
    }

    /** 人工改派（客服指定师傅）或人工触发自动改派（workerId 为空时按评分重派并排除原师傅）。 */
    @Transactional
    public void reassignManual(Long orderId, Long newWorkerId, String reason, Long operatorId) {
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        if (!OrderStatus.DISPATCHED.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "仅已派单状态的工单可以改派");
        }
        int nextRound = nextRound(orderId);
        if (nextRound > properties.getMaxReassignRounds()) {
            throw new BizException(ErrorCode.CONFLICT, "改派轮次已达上限，请联系管理员");
        }
        supersedeCurrent(orderId);

        // 回流派单池后走统一派单（排除原师傅）或直派指定师傅
        order.setStatus(OrderStatus.PENDING_DISPATCH.name());
        order.setCurrentWorkerId(null);
        repairOrderMapper.updateById(order);

        if (newWorkerId == null) {
            tryDispatch(orderId, nextRound, order.getCurrentWorkerId() == null ? null : order.getCurrentWorkerId(),
                    "人工改派：" + (reason == null ? "" : reason));
            return;
        }

        RepairOrder fresh = repairOrderMapper.selectById(orderId);
        List<CandidateWorker> pool = workerProfileMapper.selectDutyWorkers(LocalDate.now());
        CandidateWorker target = pool.stream()
                .filter(w -> w.getUserId().equals(newWorkerId))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.PARAM_INVALID, "目标师傅今日不在班，无法改派"));

        Scored scored = score(target, 0,
                target.getMaxConcurrent() == null ? properties.getMaxConcurrent() : target.getMaxConcurrent(),
                parseCategory(fresh.getCategory()), fresh);
        fresh.setStatus(OrderStatus.DISPATCHED.name());
        fresh.setCurrentWorkerId(target.getUserId());
        fresh.setDispatchMode("MANUAL");
        fresh.setAcceptDeadline(LocalDateTime.now().plusHours(properties.getArriveTimeoutHours()));
        repairOrderMapper.updateById(fresh);

        DispatchRecord record = new DispatchRecord();
        record.setOrderId(orderId);
        record.setWorkerId(target.getUserId());
        record.setMode("MANUAL");
        record.setScore(BigDecimal.valueOf(scored.score).setScale(3, RoundingMode.HALF_UP));
        record.setFactors(toJson(scored.factors));
        record.setReason("人工改派：" + (reason == null ? "" : reason));
        record.setStatus("DISPATCHED");
        record.setRoundNo(nextRound);
        record.setDispatchedBy(operatorId);
        dispatchRecordMapper.insert(record);

        recordFlow(orderId, OrderStatus.PENDING_DISPATCH, OrderStatus.DISPATCHED, OrderAction.REASSIGN, record.getReason());
    }

    /** 到场超时自动改派（定时任务入口）：排除原师傅重新评分直派。 */
    @Transactional
    public void reassignOnTimeout(Long orderId) {
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null || !OrderStatus.DISPATCHED.name().equals(order.getStatus())) {
            return;
        }
        int nextRound = nextRound(orderId);
        Long currentWorker = order.getCurrentWorkerId();
        if (nextRound > properties.getMaxReassignRounds()) {
            logDecision(order, nextRound, "到场超时自动改派", List.of(), currentWorker,
                    "改派轮次已达上限 " + properties.getMaxReassignRounds() + "，需客服人工介入", "DEGRADED", System.currentTimeMillis());
            return;
        }
        supersedeCurrent(orderId);
        order.setStatus(OrderStatus.PENDING_DISPATCH.name());
        order.setCurrentWorkerId(null);
        repairOrderMapper.updateById(order);
        tryDispatch(orderId, nextRound, currentWorker, "师傅到场超时，自动改派");
    }

    // ==================== 内部方法 ====================

    private Scored score(CandidateWorker w, long inProgress, int max, FaultCategory category, RepairOrder order) {
        double skill = skillScore(w.getSkillTags(), category);
        double load = Math.max(0.0, 1.0 - (double) inProgress / max);
        double location = w.getCommunityId() != null && w.getCommunityId().equals(order.getCommunityId()) ? 1.0 : 0.4;
        double rating = w.getRatingAvg() == null ? 0.8 : Math.min(w.getRatingAvg().doubleValue(), 5.0) / 5.0;
        double total = properties.weight("skill") * skill
                + properties.weight("load") * load
                + properties.weight("location") * location
                + properties.weight("rating") * rating;
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("skill", round3(skill));
        factors.put("load", round3(load));
        factors.put("location", round3(location));
        factors.put("rating", round3(rating));
        return new Scored(w, round3(total), factors, (int) inProgress);
    }

    /** 技能匹配度（仅作评分偏好，不限领域）：精确命中 1.0；水电↔暖通相关 0.6；其他领域 0.3。 */
    private double skillScore(String tags, FaultCategory category) {
        if (tags == null) {
            return 0.3;
        }
        if (tags.contains(category.getLabel())) {
            return 1.0;
        }
        boolean related = (category == FaultCategory.WATER_ELECTRICITY && tags.contains("暖通空调"))
                || (category == FaultCategory.HVAC && tags.contains("水电"));
        return related ? 0.6 : 0.3;
    }

    private String buildReason(Scored best) {
        return String.format("%s：技能匹配 %.0f%%，进行中 %d 单，常驻小区%s，历史评分 %s —— 综合得分 %.2f 最高，直接派单",
                best.worker.getRealName(),
                best.factors.get("skill") * 100,
                best.inProgress,
                best.factors.get("location") >= 1.0 ? "一致" : "不一致",
                best.worker.getRatingAvg() == null ? "暂无" : best.worker.getRatingAvg() + "/5",
                best.score);
    }

    private void supersedeCurrent(Long orderId) {
        DispatchRecord current = dispatchRecordMapper.selectList(new LambdaQueryWrapper<DispatchRecord>()
                        .eq(DispatchRecord::getOrderId, orderId)
                        .eq(DispatchRecord::getStatus, "DISPATCHED")
                        .orderByDesc(DispatchRecord::getRoundNo)
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
        if (current != null) {
            current.setStatus("SUPERSEDED");
            dispatchRecordMapper.updateById(current);
        }
    }

    private int nextRound(Long orderId) {
        return dispatchRecordMapper.selectList(new LambdaQueryWrapper<DispatchRecord>()
                        .eq(DispatchRecord::getOrderId, orderId)
                        .orderByDesc(DispatchRecord::getRoundNo)
                        .last("LIMIT 1"))
                .stream().findFirst().map(r -> r.getRoundNo() == null ? 1 : r.getRoundNo() + 1)
                .orElse(1);
    }

    private void recordFlow(Long orderId, OrderStatus from, OrderStatus to, OrderAction action, String remark) {
        OrderFlowRecord flow = new OrderFlowRecord();
        flow.setOrderId(orderId);
        flow.setFromStatus(from.name());
        flow.setToStatus(to.name());
        flow.setOperatorId(null);
        flow.setOperatorRole(null);
        flow.setAction(action.name());
        flow.setRemark(remark);
        orderFlowRecordMapper.insert(flow);
    }

    private void logDecision(RepairOrder order, int round, String trigger, List<Map<String, Object>> candidates,
                             Long chosen, String reason, String status, long startMs) {
        try {
            AgentDecisionLog logEntry = new AgentDecisionLog();
            logEntry.setAgentType("DISPATCH");
            logEntry.setOrderId(order.getId());
            logEntry.setMode("AUTO");
            logEntry.setInputSummary(String.format("round=%d trigger=%s category=%s", round, trigger, order.getCategory()));
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("orderId", order.getId());
            output.put("round", round);
            output.put("candidates", candidates);
            output.put("recommendation", chosen);
            output.put("reason", reason);
            output.put("mode", "AUTO");
            logEntry.setOutput(objectMapper.writeValueAsString(output));
            logEntry.setModel("rule-engine");
            logEntry.setLatencyMs((int) (System.currentTimeMillis() - startMs));
            logEntry.setStatus(status);
            agentDecisionLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("决策日志写入失败，orderId={}", order.getId(), e);
        }
    }

    private FaultCategory parseCategory(String code) {
        try {
            return FaultCategory.valueOf(code);
        } catch (Exception e) {
            return FaultCategory.OTHER;
        }
    }

    private String toJson(Map<String, Double> factors) {
        try {
            return objectMapper.writeValueAsString(factors);
        } catch (Exception e) {
            return "{}";
        }
    }

    private double round3(double v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    /** 评分结果内部载体。 */
    @Data
    private static class Scored {
        private final CandidateWorker worker;
        private final double score;
        private final Map<String, Double> factors;
        private final int inProgress;
    }
}
