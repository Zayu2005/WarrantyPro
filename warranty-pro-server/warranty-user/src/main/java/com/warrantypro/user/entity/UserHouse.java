package com.warrantypro.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 业主-房屋绑定（表 user_house，绑定需管理员审核）。 */
@Data
@TableName("user_house")
public class UserHouse {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long houseId;

    /** OWNER / TENANT / FAMILY */
    private String relation;

    /** PENDING / APPROVED / REJECTED */
    private String status;

    private Long auditedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
