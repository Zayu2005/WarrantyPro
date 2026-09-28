package com.warrantypro.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 业主服务评价（表 evaluation，一张工单只能评价一次）。 */
@Data
@TableName("evaluation")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long ownerId;

    private Long workerId;

    private Integer stars;

    /** JSON 数组字符串，例如 ["速度快","一次修复"]。 */
    private String tags;

    private String comment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
