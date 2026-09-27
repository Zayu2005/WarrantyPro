package com.warrantypro.common.result;

import java.util.List;

/**
 * 统一分页响应（docs/07 §1）：{@code { "list": [], "total": 0, "page": 1, "pageSize": 20 }}
 */
public record PageResult<T>(List<T> list, long total, long page, long pageSize) {

    public static <T> PageResult<T> of(List<T> list, long total, long page, long pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }

    public static <T> PageResult<T> empty(long page, long pageSize) {
        return new PageResult<>(List.of(), 0, page, pageSize);
    }
}
