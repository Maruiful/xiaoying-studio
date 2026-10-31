package com.xiaoying.common;

import java.util.Map;

/** 业务错误。由 GlobalExceptionHandler 按错误码转换为对应的 HTTP 状态和 ErrorVO。 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null);
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BizException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
