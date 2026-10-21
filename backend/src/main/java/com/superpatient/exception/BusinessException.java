package com.superpatient.exception;

/**
 * 业务异常：消息可直接返回给前端
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
