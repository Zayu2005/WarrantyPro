package com.warrantypro.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 物业公告（表 notice）。 */
@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 生效小区（NULL = 全局） */
    private Long communityId;

    private String title;

    private String content;

    /** 类型：停水 / 停电 / 维保 / 其他 */
    private String type;

    /** PUBLISHED / WITHDRAWN */
    private String status;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
