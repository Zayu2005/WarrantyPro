package com.warrantypro.order.controller;

import com.warrantypro.common.result.PageResult;
import com.warrantypro.common.result.Result;
import com.warrantypro.common.security.LoginUser;
import com.warrantypro.order.dto.CreateOrderRequest;
import com.warrantypro.order.dto.OrderCreateResponse;
import com.warrantypro.order.dto.OrderVO;
import com.warrantypro.order.service.RepairOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单 · 业主视角（docs/07 §2.2）。照片上传随文件服务（warranty-file）实现后接入。
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OwnerOrderController {

    private final RepairOrderService repairOrderService;

    @PostMapping
    public Result<OrderCreateResponse> create(@AuthenticationPrincipal LoginUser owner,
                                              @Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(repairOrderService.create(owner, request));
    }

    @GetMapping("/my")
    public Result<PageResult<OrderVO>> my(@AuthenticationPrincipal LoginUser owner,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(repairOrderService.myOrders(owner, status, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@AuthenticationPrincipal LoginUser user, @PathVariable Long id) {
        return Result.ok(repairOrderService.detail(user, id));
    }
}
