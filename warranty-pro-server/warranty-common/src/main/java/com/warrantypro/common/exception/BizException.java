package com.warrantypro.common.exception;

/**
 * 业务异常：由全局异常处理器转换为 {@link Result}（实现位于 warranty-bootstrap 的 GlobalExceptionHandler，实现阶段补充）。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
