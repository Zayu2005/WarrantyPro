package com.warrantypro.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 维修师傅画像（表 worker_profile，与 sys_user 一对一）。 */
@Data
@TableName("worker_profile")
public class WorkerProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** JSON 数组：故障类别标签（FaultCategory label 子集） */
    private String skillTags;

    /** 常驻小区（位置就近因子） */
    private Long communityId;

    private Integer maxConcurrent;

    /** 在岗开关：1 在岗 / 0 请假 */
    private Integer onDuty;

    /** 全期平均评分（评价写入时增量维护） */
    private BigDecimal ratingAvg;

    private Integer ratingCount;

    private Integer orderTotal;

    private Integer orderCompleted;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
