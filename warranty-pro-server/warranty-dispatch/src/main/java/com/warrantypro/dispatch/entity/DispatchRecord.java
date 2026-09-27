package com.warrantypro.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 派单记录（表 dispatch_record，一次派单一行；迭代 2 无接单环节）。 */
@Data
@TableName("dispatch_record")
public class DispatchRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long workerId;

    /** DispatchMode：AUTO / MANUAL */
    private String mode;

    private BigDecimal score;

    /** 四因子得分明细 JSON（skill/load/location/rating） */
    private String factors;

    private String reason;

    /** DispatchStatus：DISPATCHED 生效中 / SUPERSEDED 被改派替代 */
    private String status;

    /** 第几轮派单（自动改派轮次） */
    private Integer roundNo;

    private Long dispatchedBy;

    /** 派单 / 改派原因 */
    private String rejectReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
