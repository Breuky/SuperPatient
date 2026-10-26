package com.superpatient.ratelimit;

import lombok.Getter;

/** 触发限流，由全局异常处理器转成 HTTP 429 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("操作过于频繁，请 " + Math.max(1, retryAfterSeconds) + " 秒后再试");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
}
