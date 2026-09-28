package com.warrantypro.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_workflow_run")
public class WorkflowRun {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String workflowKey;
    private String workflowName;
    private Long callerId;
    private String inputSummary;
    private String status;
    private String result;
    private Integer latencyMs;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
