package com.warrantypro.warranty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warrantypro.warranty.entity.WarrantyRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WarrantyRuleMapper extends BaseMapper<WarrantyRule> {

    /**
     * 命中一条保修规则：小区专属（合同覆盖）与全局（法定默认）合并候选，按优先级取最高。
     */
    @Select("""
            SELECT * FROM warranty_rule
            WHERE deleted = 0
              AND part_category = #{partCategory}
              AND (community_id IS NULL OR community_id = #{communityId})
            ORDER BY priority DESC, id DESC
            LIMIT 1
            """)
    WarrantyRule selectRule(@Param("partCategory") String partCategory,
                            @Param("communityId") Long communityId);
}
