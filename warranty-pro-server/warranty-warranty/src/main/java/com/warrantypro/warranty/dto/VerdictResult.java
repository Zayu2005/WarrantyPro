package com.warrantypro.warranty.dto;

import com.warrantypro.common.enums.OrderStatus;
import com.warrantypro.common.enums.ResponsibleParty;
import com.warrantypro.common.enums.Verdict;

import java.time.LocalDate;

/**
 * 保修判定书（docs/03 §4 数据结构）。业主端仅展示通俗结论，完整判定随工单快照留存。
 */
public record VerdictResult(
        Verdict verdict,
        ResponsibleParty responsibleParty,
        String basis,
        LocalDate warrantyStart,
        LocalDate warrantyExpire,
        /** 受理后的路由目标状态：EXTERNAL_PROCESSING（保修期内）/ PENDING_DISPATCH（保修期外） */
        OrderStatus routeTo
) {
}
