package com.warrantypro.agent.dto;

import java.time.LocalDateTime;

public record WorkflowRunVO(
        Long id,
        String workflowKey,
        String workflowName,
        Long callerId,
        String inputSummary,
        String status,
        String result,
        Integer latencyMs,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt
) {
}
