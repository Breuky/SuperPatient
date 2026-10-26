package com.superpatient.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 在接口上声明限流。可重复使用，例如「每天 20 次 + 每分钟 3 次」。
 *
 * 只影响带了注解的方法：没注解的接口不做任何额外操作。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RateLimits.class)
public @interface RateLimit {

    /** 计数前缀，同一接口的多个规则要用不同 scope */
    String scope();

    /** 窗口内允许的次数 */
    int limit();

    /** 窗口长度，配合 unit */
    long window() default 1;

    TimeUnit unit() default TimeUnit.MINUTES;

    Dimension dimension() default Dimension.USER;

    /**
     * Redis 不可用时是否放行。
     * true（默认）：登录这类要保住可用性的接口；
     * false：烧钱的接口（识别、解读）宁可拒绝。
     */
    boolean failOpen() default true;
}
