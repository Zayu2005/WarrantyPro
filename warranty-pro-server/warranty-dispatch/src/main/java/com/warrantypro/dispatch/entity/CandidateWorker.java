package com.warrantypro.dispatch.entity;

import lombok.Data;

import java.math.BigDecimal;

/** 候选师傅（派单评分中间对象，join 排班与用户表得出）。 */
@Data
public class CandidateWorker {

    private Long userId;

    private String realName;

    private Long communityId;

    private Integer maxConcurrent;

    private BigDecimal ratingAvg;
}
