package com.warrantypro.order.controller;

import com.warrantypro.common.result.PageResult;
import com.warrantypro.common.result.Result;
import com.warrantypro.common.security.LoginUser;
import com.warrantypro.order.dto.OrderVO;
import com.warrantypro.order.service.RepairOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单 · 师傅视角（docs/07 §2.4）。接单 / 进度 / 完工提交随迭代 2 接入。
 */
@RestController
@RequestMapping("/api/v1/worker/orders")
@RequiredArgsConstructor
public class WorkerOrderController {

    private final RepairOrderService repairOrderService;

    @GetMapping
    @PreAuthorize("hasRole('WORKER')")
    public Result<PageResult<OrderVO>> todo(@AuthenticationPrincipal LoginUser worker,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(repairOrderService.workerOrders(worker, page, pageSize));
    }
}
