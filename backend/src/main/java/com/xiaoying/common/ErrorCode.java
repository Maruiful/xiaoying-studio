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
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "用户不存在"),

    ASSET_EMPTY(HttpStatus.BAD_REQUEST, "图片文件为空"),
    ASSET_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "图片超过大小限制"),
    ASSET_TYPE_NOT_ALLOWED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的图片格式"),
    ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "素材不存在"),

    TASK_ASSET_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, "素材数量与模板要求不符"),
    TASK_ASSET_INVALID(HttpStatus.BAD_REQUEST, "素材不可用"),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "任务不存在"),
    RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "制作结果不存在");

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
