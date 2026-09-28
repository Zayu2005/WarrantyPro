package com.warrantypro.agent.dto;

import java.time.LocalDateTime;

public record WorkflowNodeLogVO(
        Long id,
        Long runId,
        String nodeKey,
        String nodeName,
        Integer sequenceNo,
        String status,
        String inputSummary,
        String outputSummary,
        String model,
        Integer latencyMs,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt
) {
}
