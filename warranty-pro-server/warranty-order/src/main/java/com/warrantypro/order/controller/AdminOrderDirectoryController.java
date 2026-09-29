package com.warrantypro.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warrantypro.common.result.PageResult;
import com.warrantypro.common.result.Result;
import com.warrantypro.order.entity.Evaluation;
import com.warrantypro.order.entity.RepairOrder;
import com.warrantypro.order.mapper.EvaluationMapper;
import com.warrantypro.order.mapper.RepairOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only evaluation and completed-order directories for property operations. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','DISPATCHER')")
public class AdminOrderDirectoryController {

    private final EvaluationMapper evaluationMapper;
    private final RepairOrderMapper repairOrderMapper;

    @GetMapping("/evaluations")
    public Result<PageResult<Evaluation>> evaluations(@RequestParam(required = false) Integer stars,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long pageSize) {
        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        Page<Evaluation> result = evaluationMapper.selectPage(new Page<>(safePage, safePageSize),
                new LambdaQueryWrapper<Evaluation>()
                        .eq(stars != null, Evaluation::getStars, stars)
                        .orderByDesc(Evaluation::getCreatedAt));
        return Result.ok(PageResult.of(result.getRecords(), result.getTotal(), safePage, safePageSize));
    }

    @GetMapping("/archives")
    public Result<PageResult<RepairOrder>> archives(@RequestParam(required = false) String keyword,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "20") long pageSize) {
        String trimmed = keyword == null ? "" : keyword.trim();
        long safePage = Math.max(1, page);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        Page<RepairOrder> result = repairOrderMapper.selectPage(new Page<>(safePage, safePageSize),
                new LambdaQueryWrapper<RepairOrder>()
                        .eq(RepairOrder::getStatus, "COMPLETED")
                        .eq(RepairOrder::getDeleted, 0)
                        .and(!trimmed.isEmpty(), q -> q.like(RepairOrder::getOrderNo, trimmed)
                                .or().like(RepairOrder::getLocationDetail, trimmed))
                        .orderByDesc(RepairOrder::getCompletedAt));
        return Result.ok(PageResult.of(result.getRecords(), result.getTotal(), safePage, safePageSize));
    }
}
