package com.warrantypro.common.result;

/**
 * 统一响应体（docs/07 §1）：
 * <pre>{ "code": 0, "message": "ok", "data": { } }</pre>
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> fail(ErrorCodeHolder errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getDefaultMessage(), null);
    }

    public static <T> Result<T> fail(ErrorCodeHolder errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    /**
     * 错误码持有接口：便于业务模块扩展自有错误码枚举而不与通用枚举耦合。
     */
    public interface ErrorCodeHolder {
        int getCode();

        String getDefaultMessage();
    }
}
