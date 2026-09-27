package com.warrantypro.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** Agent 决策日志（表 agent_decision_log，可审计可回放，docs/05 §4.2）。 */
@Data
@TableName("agent_decision_log")
public class AgentDecisionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String agentType;

    private Long orderId;

    /** RECOMMEND / AUTO（派单场景为 AUTO） */
    private String mode;

    private String inputSummary;

    /** 决策输出 JSON */
    private String output;

    /** 使用的模型标识（规则引擎阶段为 rule-engine） */
    private String model;

    private Integer latencyMs;

    /** OK / SCHEMA_FAIL / DEGRADED */
    private String status;

    private LocalDateTime createdAt;
}
