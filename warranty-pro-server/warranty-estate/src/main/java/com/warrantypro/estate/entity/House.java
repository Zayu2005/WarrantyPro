package com.warrantypro.estate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 房屋（表 house）。 */
@Data
@TableName("house")
public class House {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long buildingId;

    private String unit;

    private String roomNo;

    private BigDecimal area;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
