package com.warrantypro.agent.dto;

import java.util.List;

public record WorkflowDefinitionVO(
        String workflowKey,
        String workflowName,
        List<WorkflowNodeVO> nodes,
        List<WorkflowEdgeVO> edges
) {
    public record WorkflowNodeVO(String key, String name, String type, int order) {
    }

    public record WorkflowEdgeVO(String from, String to, String label) {
    }
}
