package com.warrantypro.warranty.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 保修期限规则（表 warranty_rule，判定引擎数据源，docs/03 §4）。 */
@Data
@TableName("warranty_rule")
public class WarrantyRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** LEGAL 法定默认 / CONTRACT 合同覆盖 */
    private String scope;

    /** 工程部位：MAIN_STRUCTURE/WATERPROOF/HEATING_COOLING/ME_INSTALLATION/INSULATION */
    private String partCategory;

    private Integer durationValue;

    /** YEAR / HEATING_SEASON / DESIGN_LIFE */
    private String durationUnit;

    /** NULL = 全局生效 */
    private Long communityId;

    /** 条款依据 */
    private String source;

    /** 合同 > 法定，同 scope 内数值大者优先 */
    private Integer priority;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
