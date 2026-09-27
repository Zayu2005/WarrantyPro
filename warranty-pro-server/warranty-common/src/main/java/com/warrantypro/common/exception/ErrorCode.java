package com.warrantypro.common.exception;

import com.warrantypro.common.result.Result;

/**
 * 全局错误码（docs/07 §5，全模块统一口径）。
 */
public enum ErrorCode implements Result.ErrorCodeHolder {

    UNAUTHORIZED(40101, "未登录或登录已失效"),
    FORBIDDEN(40301, "无权限"),
    NOT_FOUND(40401, "资源不存在"),
    CONFLICT(40901, "状态冲突"),
    PARAM_INVALID(42201, "参数校验失败"),
    TOO_MANY_REQUESTS(42901, "请求过于频繁"),
    SYSTEM_ERROR(50001, "系统异常"),
    AI_UNAVAILABLE(50301, "AI 服务不可用，已降级"),
    AI_OUTPUT_INVALID(50302, "模型输出校验失败");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
