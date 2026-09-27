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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单 · 客服调度视角（docs/07 §2.3）。判定纠正 / 派单 / 催办随迭代 2 接入。
 */
@RestController
@RequestMapping("/api/v1/dispatch/orders")
@RequiredArgsConstructor
public class DispatchOrderController {

    private final RepairOrderService repairOrderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','ADMIN')")
    public Result<PageResult<OrderVO>> pool(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) Long communityId,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(repairOrderService.dispatchPool(status, category, communityId, page, pageSize));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public Result<OrderVO> accept(@AuthenticationPrincipal LoginUser dispatcher, @PathVariable Long id) {
        return Result.ok(repairOrderService.accept(dispatcher, id));
    }
}
