package com.warrantypro.estate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 楼栋（表 building，completion_date 为户内保修判定起算日）。 */
@Data
@TableName("building")
public class Building {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long communityId;

    private String name;

    private LocalDate completionDate;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
