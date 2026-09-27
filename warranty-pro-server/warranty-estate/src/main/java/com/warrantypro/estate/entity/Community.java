package com.warrantypro.estate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 小区（表 community）。 */
@Data
@TableName("community")
public class Community {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String address;

    /** 开发商名称（保修责任对接方） */
    private String developerName;

    private String contact;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
