package com.warrantypro.agent.controller;

import com.warrantypro.agent.dto.RepairWorkflowRequest;
import com.warrantypro.agent.dto.WorkflowDefinitionVO;
import com.warrantypro.agent.dto.WorkflowNodeLogVO;
import com.warrantypro.agent.dto.WorkflowRunVO;
import com.warrantypro.agent.service.RepairWorkflowService;
import com.warrantypro.common.exception.BizException;
import com.warrantypro.common.exception.ErrorCode;
import com.warrantypro.common.result.Result;
import com.warrantypro.common.security.LoginUser;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent/workflows")
@RequiredArgsConstructor
public class AgentWorkflowController {

    private final RepairWorkflowService workflowService;

    @GetMapping("/definition")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DISPATCHER')")
    public Result<WorkflowDefinitionVO> definition() {
        return Result.ok(workflowService.definition());
    }

    @PostMapping("/repair-triage/runs")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DISPATCHER','OWNER','WORKER')")
    public Result<WorkflowRunVO> start(@AuthenticationPrincipal LoginUser caller,
                                       @Valid @RequestBody RepairWorkflowRequest request) {
        return Result.ok(workflowService.start(caller, request));
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DISPATCHER')")
    public Result<List<WorkflowRunVO>> runs(@RequestParam(defaultValue = "30") int limit) {
        return Result.ok(workflowService.listRuns(limit));
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DISPATCHER')")
    public Result<WorkflowRunVO> run(@PathVariable Long id) {
        WorkflowRunVO run = workflowService.getRun(id);
        if (run == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工作流运行记录不存在");
        }
        return Result.ok(run);
    }

    @GetMapping("/runs/{id}/logs")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DISPATCHER')")
    public Result<List<WorkflowNodeLogVO>> logs(@PathVariable Long id) {
        if (workflowService.getRun(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "工作流运行记录不存在");
        }
        return Result.ok(workflowService.listNodeLogs(id));
    }
}
