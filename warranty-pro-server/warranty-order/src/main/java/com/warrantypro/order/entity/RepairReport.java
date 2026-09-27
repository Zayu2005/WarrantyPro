package com.warrantypro.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 维修记录（表 repair_report，与工单一对一）。 */
@Data
@TableName("repair_report")
public class RepairReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long workerId;

    private String faultCause;

    private String measures;

    /** 更换材料清单 JSON */
    private String materials;

    private BigDecimal workHours;

    /** 是否 AI 辅助生成 */
    private Integer aiAssisted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
