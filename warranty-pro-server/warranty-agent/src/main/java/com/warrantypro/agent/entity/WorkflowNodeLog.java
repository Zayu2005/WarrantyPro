package com.warrantypro.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_workflow_node_log")
public class WorkflowNodeLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private String nodeKey;
    private String nodeName;
    private Integer sequenceNo;
    private String status;
    private String inputSummary;
    private String outputSummary;
    private String model;
    private Integer latencyMs;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
