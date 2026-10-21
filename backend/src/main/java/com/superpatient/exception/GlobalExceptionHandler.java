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
     * 路径不存在时返回真正的 404，而不是被兜底成 200。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error("资源不存在"));
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("未处理异常", e);
        return Result.error("服务器内部错误: " + e.getMessage());
    }
}
