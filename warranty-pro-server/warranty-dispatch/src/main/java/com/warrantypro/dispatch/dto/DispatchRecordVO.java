package com.warrantypro.dispatch.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 派单记录 VO（含师傅姓名）。 */
@Data
public class DispatchRecordVO {

    private Long id;

    private Integer roundNo;

    /** AUTO / MANUAL */
    private String mode;

    private Long workerId;

    private String workerName;

    private BigDecimal score;

    private String factors;

    private String reason;

    /** DISPATCHED / SUPERSEDED */
    private String status;

    private String createdAt;
}
