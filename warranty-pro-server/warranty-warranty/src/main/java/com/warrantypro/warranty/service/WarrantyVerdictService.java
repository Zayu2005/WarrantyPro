package com.warrantypro.warranty.service;

import com.warrantypro.common.enums.FaultCategory;
import com.warrantypro.common.enums.ObjectType;
import com.warrantypro.common.enums.OrderStatus;
import com.warrantypro.common.enums.ResponsibleParty;
import com.warrantypro.common.enums.Verdict;
import com.warrantypro.common.exception.BizException;
import com.warrantypro.common.exception.ErrorCode;
import com.warrantypro.estate.entity.Building;
import com.warrantypro.estate.entity.Facility;
import com.warrantypro.estate.entity.House;
import com.warrantypro.estate.mapper.BuildingMapper;
import com.warrantypro.estate.mapper.FacilityMapper;
import com.warrantypro.estate.mapper.HouseMapper;
import com.warrantypro.warranty.dto.VerdictResult;
import com.warrantypro.warranty.entity.WarrantyRule;
import com.warrantypro.warranty.mapper.WarrantyRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 保修判定引擎（docs/03 §4）。
 *
 * <p>纯规则实现，零 AI 依赖、100% 确定可解释：依据《建设工程质量管理条例》法定期限规则 +
 * 物业合同覆盖规则（priority 更高者胜出），以楼栋竣工日期（户内）或设施台账保修起始日
 * （公共设施）为起算日，判定工单责任方与路由目标。人为损坏（OWNER_RESPONSIBLE）由客服
 * 受理时人工纠正，不在引擎自动判定范围内。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarrantyVerdictService {

    /** 故障类别 → 保修规则工程部位的映射（docs/06 §3.2 part_category 口径） */
    private static final Map<FaultCategory, String> CATEGORY_TO_PART = Map.of(
            FaultCategory.CIVIL_WATERPROOF, "WATERPROOF",
            FaultCategory.HVAC, "HEATING_COOLING",
            FaultCategory.WATER_ELECTRICITY, "ME_INSTALLATION",
            FaultCategory.DOOR_WINDOW, "ME_INSTALLATION",
            FaultCategory.ELEVATOR, "ME_INSTALLATION",
            FaultCategory.PUBLIC_FACILITY, "ME_INSTALLATION",
            FaultCategory.OTHER, "ME_INSTALLATION"
    );

    private final HouseMapper houseMapper;
    private final BuildingMapper buildingMapper;
    private final FacilityMapper facilityMapper;
    private final WarrantyRuleMapper warrantyRuleMapper;

    /**
     * 判定一份工单的保修责任。
     *
     * @param objectType 报修对象（户内 / 公共设施）
     * @param category   故障类别
     * @param houseId    户内报修时的房屋 ID
     * @param facilityId 公共设施报修时的设施 ID
     */
    public VerdictResult judge(ObjectType objectType, FaultCategory category, Long houseId, Long facilityId) {
        if (objectType == ObjectType.PUBLIC_FACILITY) {
            return judgeFacility(facilityId);
        }
        return judgeIndoor(houseId, category);
    }

    private VerdictResult judgeIndoor(Long houseId, FaultCategory category) {
        if (houseId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "户内报修必须指定房屋");
        }
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "报修房屋不存在");
        }
        Building building = buildingMapper.selectById(house.getBuildingId());
        if (building == null || building.getCompletionDate() == null) {
            return outOfWarranty("楼栋竣工日期未配置，无法认定保修期，按保修期外处理");
        }

        LocalDate start = building.getCompletionDate();
        String part = CATEGORY_TO_PART.getOrDefault(category, "ME_INSTALLATION");
        WarrantyRule rule = warrantyRuleMapper.selectRule(part, building.getCommunityId());
        if (rule == null) {
            return outOfWarranty("未匹配到「" + part + "」的保修规则，按保修期外处理");
        }
        if ("DESIGN_LIFE".equals(rule.getDurationUnit())) {
            return new VerdictResult(Verdict.IN_WARRANTY, ResponsibleParty.DEVELOPER,
                    rule.getSource() + "（设计使用年限内）", start, null, OrderStatus.EXTERNAL_PROCESSING);
        }

        LocalDate end = expire(start, rule);
        String basis = rule.getSource() + "；保修期 " + start + " 至 " + end;
        return buildVerdict(start, end, basis);
    }

    private VerdictResult judgeFacility(Long facilityId) {
        if (facilityId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "公共设施报修必须指定设施");
        }
        Facility facility = facilityMapper.selectById(facilityId);
        if (facility == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "报修设施不存在");
        }
        if (facility.getWarrantyStart() == null || facility.getWarrantyEnd() == null) {
            return outOfWarranty("设施台账缺少保修起止日，按保修期外处理");
        }
        String basis = "设施台账「" + facility.getName() + "」保修期 "
                + facility.getWarrantyStart() + " 至 " + facility.getWarrantyEnd();
        return buildVerdict(facility.getWarrantyStart(), facility.getWarrantyEnd(), basis);
    }

    /** 到期日计算：自然年精确；采暖期/供冷期按自然年近似（演示口径，合同规则可覆盖）。 */
    private LocalDate expire(LocalDate start, WarrantyRule rule) {
        int value = rule.getDurationValue() == null ? 0 : rule.getDurationValue();
        return start.plusYears(value);
    }

    private VerdictResult buildVerdict(LocalDate start, LocalDate end, String basis) {
        if (!LocalDate.now().isAfter(end)) {
            return new VerdictResult(Verdict.IN_WARRANTY, ResponsibleParty.DEVELOPER, basis,
                    start, end, OrderStatus.EXTERNAL_PROCESSING);
        }
        return new VerdictResult(Verdict.OUT_OF_WARRANTY, ResponsibleParty.PROPERTY,
                basis + "（已过保）", start, end, OrderStatus.PENDING_DISPATCH);
    }

    private VerdictResult outOfWarranty(String basis) {
        return new VerdictResult(Verdict.OUT_OF_WARRANTY, ResponsibleParty.PROPERTY, basis,
                null, null, OrderStatus.PENDING_DISPATCH);
    }
}
