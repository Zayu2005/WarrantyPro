package com.warrantypro.order.controller;

import com.warrantypro.common.result.PageResult;
import com.warrantypro.common.result.Result;
import com.warrantypro.common.security.LoginUser;
import com.warrantypro.order.dto.OrderVO;
import com.warrantypro.order.service.RepairOrderService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 工单 · 师傅视角（docs/07 §2.4，迭代 2：无接单环节，到场打卡 + 完工提交）。
 */
@RestController
@RequestMapping("/api/v1/worker/orders")
@RequiredArgsConstructor
public class WorkerOrderController {

    private final RepairOrderService repairOrderService;

    @GetMapping
    @PreAuthorize("hasRole('WORKER')")
    public Result<PageResult<OrderVO>> todo(@AuthenticationPrincipal LoginUser worker,
                                            @RequestParam(defaultValue = "false") boolean history,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(repairOrderService.workerOrders(worker, history, page, pageSize));
    }

    /** 到场打卡：已派单·待上门 → 维修中。 */
    @PostMapping("/{id}/arrive")
    @PreAuthorize("hasRole('WORKER')")
    public Result<OrderVO> arrive(@AuthenticationPrincipal LoginUser worker, @PathVariable Long id) {
        return Result.ok(repairOrderService.arrive(worker, id));
    }

    /** 完工提交：维修记录入库 → 待验收。 */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('WORKER')")
    public Result<OrderVO> complete(@AuthenticationPrincipal LoginUser worker,
                                    @PathVariable Long id,
                                    @RequestBody CompleteRequest body) {
        return Result.ok(repairOrderService.complete(worker, id,
                body.getFaultCause(), body.getMeasures(), body.getWorkHours()));
    }

    @Data
    public static class CompleteRequest {
        @NotBlank(message = "故障原因不能为空")
        private String faultCause;
        @NotBlank(message = "处理措施不能为空")
        private String measures;
        private BigDecimal workHours;
    }
}
