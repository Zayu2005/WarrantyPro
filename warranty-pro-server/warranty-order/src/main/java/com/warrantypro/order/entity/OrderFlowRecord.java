package com.warrantypro.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 工单流转记录（表 order_flow_record，业主端时间轴与催办依据）。 */
@Data
@TableName("order_flow_record")
public class OrderFlowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String fromStatus;

    private String toStatus;

    private Long operatorId;

    private String operatorRole;

    /** 动作（OrderAction） */
    private String action;

    private String remark;

    private LocalDateTime createdAt;
}
