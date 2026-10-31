package com.xiaoying.common;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 把所有异常转换为统一的 ErrorVO。
 *
 * <p>继承 ResponseEntityExceptionHandler，让 405、415、404（无路由）、上传超限等框架异常保留正确的状态码，只替换响应体格式。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorVO> handleBiz(BizException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.status()).body(ErrorVO.of(code, e.getMessage(), e.getDetails()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorVO> handleUnexpected(Exception e) {
        // 详细信息只写日志，不把堆栈或 SQL 返回给前端
        log.error("未预期的异常", e);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.status()).body(ErrorVO.of(code, code.defaultMessage(), null));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, Object> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(code.status()).body(ErrorVO.of(code, code.defaultMessage(), fields));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorCode code = ErrorCode.fromStatus(status.value());
        if (status.is5xxServerError()) {
            log.error("框架层异常", e);
        }
        return ResponseEntity.status(status).headers(headers).body(ErrorVO.of(code, code.defaultMessage(), null));
    }
}
