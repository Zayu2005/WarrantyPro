package com.warrantypro.dispatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warrantypro.common.exception.BizException;
import com.warrantypro.common.exception.ErrorCode;
import com.warrantypro.common.result.Result;
import com.warrantypro.common.security.LoginUser;
import com.warrantypro.dispatch.dto.DispatchRecordVO;
import com.warrantypro.dispatch.entity.CandidateWorker;
import com.warrantypro.dispatch.entity.WorkerSchedule;
import com.warrantypro.dispatch.mapper.DispatchRecordMapper;
import com.warrantypro.dispatch.mapper.WorkerProfileMapper;
import com.warrantypro.dispatch.mapper.WorkerScheduleMapper;
import com.warrantypro.dispatch.service.DispatchService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员端：师傅排班 + 派单记录 + 人工改派（docs/02 FR-A-09、FR-D-04）。
 */
@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
public class DispatchAdminController {

    private final WorkerProfileMapper workerProfileMapper;
    private final WorkerScheduleMapper workerScheduleMapper;
    private final DispatchRecordMapper dispatchRecordMapper;
    private final DispatchService dispatchService;

    /** 师傅列表（含今日在班标记）。 */
    @GetMapping("/workers")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','MANAGER')")
    public Result<List<Map<String, Object>>> workers() {
        LocalDate today = LocalDate.now();
        Set<Long> todayDuty = workerProfileMapper.selectDutyWorkers(today).stream()
                .map(CandidateWorker::getUserId)
                .collect(Collectors.toSet());
        List<Map<String, Object>> list = new ArrayList<>();
        for (CandidateWorker w : workerProfileMapper.selectAllWorkers()) {
            list.add(Map.of(
                    "workerId", w.getUserId(),
                    "realName", w.getRealName(),
                    "todayDuty", todayDuty.contains(w.getUserId())));
        }
        return Result.ok(list);
    }

    /** 排班查询（区间）。 */
    @GetMapping("/schedule")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','MANAGER')")
    public Result<List<Map<String, Object>>> schedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(defaultValue = "7") int days) {
        LocalDate end = start.plusDays(Math.max(1, days) - 1L);
        List<Map<String, Object>> rows = workerScheduleMapper.selectRange(start, end).stream()
                .map(s -> Map.<String, Object>of(
                        "workerId", s.getWorkerId(),
                        "dutyDate", s.getDutyDate().toString()))
                .toList();
        return Result.ok(rows);
    }

    /** 排班切换（管理员勾选/取消某师傅某天的班）。 */
    @PutMapping("/schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggle(@RequestBody ScheduleToggle body) {
        if (body.workerId == null || body.dutyDate == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "参数不完整");
        }
        boolean exists = workerScheduleMapper.selectCount(new LambdaQueryWrapper<WorkerSchedule>()
                .eq(WorkerSchedule::getWorkerId, body.workerId)
                .eq(WorkerSchedule::getDutyDate, body.dutyDate)) > 0;
        if (body.onDuty && !exists) {
            WorkerSchedule schedule = new WorkerSchedule();
            schedule.setWorkerId(body.workerId);
            schedule.setDutyDate(body.dutyDate);
            schedule.setShift("FULL");
            workerScheduleMapper.insert(schedule);
        } else if (!body.onDuty && exists) {
            workerScheduleMapper.delete(new LambdaQueryWrapper<WorkerSchedule>()
                    .eq(WorkerSchedule::getWorkerId, body.workerId)
                    .eq(WorkerSchedule::getDutyDate, body.dutyDate));
        }
        return Result.ok();
    }

    /** 智能一键排班：为未来 days 天生成均衡轮转的值班表。 */
    @PostMapping("/schedule/auto-generate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> autoGenerate(@RequestBody AutoGenerateBody body) {
        int generated = dispatchService.generateSchedule(
                body.days == null ? 30 : body.days,
                body.perDay == null ? 2 : body.perDay);
        return Result.ok(Map.of(
                "generated", generated,
                "days", body.days == null ? 30 : body.days,
                "perDay", body.perDay == null ? 2 : body.perDay));
    }

    /** 派单记录（轮次 / 得分 / 理由）。 */
    @GetMapping("/orders/{id}/records")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','MANAGER')")
    public Result<List<DispatchRecordVO>> records(@PathVariable Long id) {
        return Result.ok(dispatchRecordMapper.selectVOByOrder(id));
    }

    /** 人工改派：workerId 为空时触发智能体重派（排除原师傅）。 */
    @PostMapping("/orders/{id}/reassign")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public Result<Void> reassign(@AuthenticationPrincipal LoginUser operator,
                                 @PathVariable Long id,
                                 @RequestBody ReassignBody body) {
        dispatchService.reassignManual(id, body.workerId, body.reason, operator.userId());
        return Result.ok();
    }

    /** 待派单工单人工指定在班师傅，供自动派单无候选时客服兜底处理。 */
    @PostMapping("/orders/{id}/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public Result<Void> dispatchPending(@AuthenticationPrincipal LoginUser operator,
                                        @PathVariable Long id,
                                        @RequestBody ManualDispatchBody body) {
        dispatchService.dispatchPendingManual(id, body.workerId, body.reason, operator.userId());
        return Result.ok();
    }

    @Data
    public static class ScheduleToggle {
        private Long workerId;
        private LocalDate dutyDate;
        private Boolean onDuty;
    }

    @Data
    public static class ReassignBody {
        private Long workerId;
        private String reason;
    }

    @Data
    public static class ManualDispatchBody {
        private Long workerId;
        private String reason;
    }

    @Data
    public static class AutoGenerateBody {
        /** 生成天数（默认 30，上限 90） */
        private Integer days;
        /** 每日值班人数（默认 2） */
        private Integer perDay;
    }
}
