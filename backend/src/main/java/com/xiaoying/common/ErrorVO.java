package com.xiaoying.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** 统一错误响应，格式见 docs/conventions.md 2.4。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorVO(String code, String message, Map<String, Object> details) {

    public static ErrorVO of(ErrorCode errorCode, String message, Map<String, Object> details) {
        return new ErrorVO(errorCode.name(), message, details);
    }
}
