package com.superpatient.exception;

import com.superpatient.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(e.getMessage());
    }

    /**
     * 触发限流返回真正的 429，并带 Retry-After，前端可据此提示等待时间。
     */
    @ExceptionHandler(com.superpatient.ratelimit.RateLimitExceededException.class)
    public ResponseEntity<Result> handleRateLimit(com.superpatient.ratelimit.RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                .body(Result.error(e.getMessage()));
    }

    /**
     * 路径不存在时返回真正的 404，而不是被兜底成 200。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error("资源不存在"));
    }

    /**
     * 请求体格式不对（例如把 multipart 接口用 JSON 调）：返回 415 并说清楚要什么，
     * 而不是被兜底成「服务器内部错误」。
     */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result> handleMediaType(org.springframework.web.HttpMediaTypeNotSupportedException e) {
        String expected = e.getSupportedMediaTypes().isEmpty()
                ? "" : "，需要 " + e.getSupportedMediaTypes().get(0).toString();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Result.error("请求格式不正确" + expected));
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("未处理异常", e);
        return Result.error("服务器内部错误: " + e.getMessage());
    }
}
