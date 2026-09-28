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
        /** 旧客户端兼容字段；新工单始终由客服受理后进入统一派单流程。 */
        OrderStatus routeTo
) {
}
