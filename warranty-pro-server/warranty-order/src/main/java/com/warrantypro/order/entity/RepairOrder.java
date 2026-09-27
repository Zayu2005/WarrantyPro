package com.warrantypro.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 报修工单（表 repair_order，状态机见 docs/03 §1，判定结果冗余快照）。 */
@Data
@TableName("repair_order")
public class RepairOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单号：WO + yyyyMMdd + 4 位序列 */
    private String orderNo;

    private Long communityId;

    private Long houseId;

    private Long facilityId;

    private Long ownerId;

    /** INDOOR / PUBLIC_FACILITY */
    private String objectType;

    /** 故障类别（FaultCategory） */
    private String category;

    private String locationDetail;

    private String phenomenon;

    /** URGENT / NORMAL */
    private String urgency;

    /** AI_CHAT / FORM */
    private String source;

    /** AI 报修来源会话（追溯） */
    private Long sourceSessionId;

    /** 状态机 8 态（OrderStatus） */
    private String status;

    /** 判定快照：Verdict */
    private String verdict;

    /** 判定快照：ResponsibleParty */
    private String responsibleParty;

    /** 判定快照：依据条款 */
    private String verdictBasis;

    /** 判定快照：起算日 */
    private LocalDate warrantyStart;

    /** 判定快照：到期日 */
    private LocalDate warrantyExpire;

    /** 客服纠正判定时的操作人 */
    private Long verdictAdjustedBy;

    /** PaidStatus：NOT_REQUIRED / UNPAID / PAID */
    private String paidStatus;

    /** 有偿维修金额 */
    private BigDecimal feeAmount;

    private Long currentWorkerId;

    /** DispatchMode：RECOMMEND / AUTO / MANUAL */
    private String dispatchMode;

    /** 接单截止时间（超时自动改派） */
    private LocalDateTime acceptDeadline;

    private LocalDateTime submittedAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime dispatchedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime confirmedAt;

    private String cancelReason;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
