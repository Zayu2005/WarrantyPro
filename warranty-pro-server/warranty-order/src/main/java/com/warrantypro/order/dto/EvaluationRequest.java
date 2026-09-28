package com.warrantypro.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 完结工单评价请求。 */
public record EvaluationRequest(
        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分最低为 1 星")
        @Max(value = 5, message = "评分最高为 5 星")
        Integer stars,
        @Size(max = 5, message = "评价标签最多 5 个")
        List<@Size(max = 20, message = "评价标签过长") String> tags,
        @Size(max = 500, message = "评价内容不能超过 500 字")
        String comment
) {
}
