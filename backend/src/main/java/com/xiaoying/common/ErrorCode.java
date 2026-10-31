package com.xiaoying.common;

import org.springframework.http.HttpStatus;

/** 所有错误码。code 给程序判断，取值固定；message 是给用户看的默认文案。 */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "请求参数不合法"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "不支持该请求方法"),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "请求内容过大"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的内容类型"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"),

    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "模板不存在"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "用户不存在");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    /** 框架层异常（405、415 等）没有业务错误码时，按 HTTP 状态归类。 */
    static ErrorCode fromStatus(int status) {
        return switch (status) {
            case 400 -> VALIDATION_FAILED;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> status >= 400 && status < 500 ? VALIDATION_FAILED : INTERNAL_ERROR;
        };
    }
}
