package com.warrantypro.estate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 设施设备台账（表 facility，保修起止日为公共设施判定数据源）。 */
@Data
@TableName("facility")
public class Facility {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long communityId;

    private String name;

    /** ELEVATOR / FIRE / PUMP / FACADE / HVAC / OTHER */
    private String type;

    private String location;

    private String supplierName;

    private String contactName;

    private String contactPhone;

    private LocalDate installDate;

    private LocalDate warrantyStart;

    private LocalDate warrantyEnd;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
