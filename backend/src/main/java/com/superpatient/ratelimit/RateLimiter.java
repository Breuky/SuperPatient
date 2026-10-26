package com.superpatient.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 滑动窗口限流：Redis 用 ZSET 记每次请求的时间戳，Lua 保证「清理 + 计数 + 写入」是原子的。
 *
 * 为什么用滑动窗口而不是固定窗口：固定窗口在边界会放过两倍流量
 * （59 秒打满、60 秒又能打满），登录这种场景不能有这种缺口。
 *
 * Redis 不可用时按调用方给的 failOpen 决定放行还是拒绝：
 * 登录 fail-open（可用性优先），识别/解读 fail-closed（成本优先）。
 */
@Slf4j
@Service
public class RateLimiter {

    private static final String SCRIPT = """
            local key    = KEYS[1]
            local now    = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit  = tonumber(ARGV[3])

            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            local count = redis.call('ZCARD', key)
            if count >= limit then
                local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                local wait = window - (now - tonumber(oldest[2]))
                if wait < 0 then wait = 0 end
                return {0, wait}
            end
            redis.call('ZADD', key, now, ARGV[4])
            redis.call('PEXPIRE', key, window)
            return {1, 0}
            """;

    private final StringRedisTemplate redis;
    private final LocalWindowCounter local = new LocalWindowCounter();
    private final DefaultRedisScript<List> script;
    private final boolean enabled;
    /** 避免 Redis 持续不可用时刷屏，每分钟最多一条 warn */
    private final AtomicLong lastWarnMillis = new AtomicLong(0);

    public RateLimiter(StringRedisTemplate redis,
                       @Value("${app.rate-limit.enabled:true}") boolean enabled) {
        this.redis = redis;
        this.enabled = enabled;
        this.script = new DefaultRedisScript<>(SCRIPT, List.class);
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
        static Decision allow() {
            return new Decision(true, 0);
        }

        static Decision deny(long retryAfterSeconds) {
            return new Decision(false, Math.max(1, retryAfterSeconds));
        }
    }

    public Decision check(String key, int limit, Duration window, boolean failOpen) {
        if (!enabled) {
            return Decision.allow();
        }
        try {
            List<?> result = redis.execute(script, List.of(key),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(window.toMillis()),
                    String.valueOf(limit),
                    System.currentTimeMillis() + ":" + UUID.randomUUID());
            if (result == null || result.size() < 2) {
                return failOpen ? Decision.allow() : Decision.deny(window.toSeconds());
            }
            boolean allowed = toLong(result.get(0)) == 1L;
            long waitMillis = toLong(result.get(1));
            return allowed ? Decision.allow() : Decision.deny(ceilSeconds(waitMillis));
        } catch (Exception e) {
            warnOnce(e);
            // Redis 挂了仍然限流，只是退化成「每个实例各自计数」：
            // 多实例下额度会放宽，但远好过直接拒绝或完全不限。
            try {
                long waitMillis = local.hitAndRetryAfter(key, limit, window, System.currentTimeMillis());
                return waitMillis < 0 ? Decision.allow() : Decision.deny(ceilSeconds(waitMillis));
            } catch (Exception fallbackFailure) {
                return failOpen ? Decision.allow() : Decision.deny(Math.min(window.toSeconds(), 60));
            }
        }
    }

    /** 登录成功后清掉失败计数 */
    public void reset(String key) {
        if (!enabled) return;
        try {
            redis.delete(key);
        } catch (Exception e) {
            warnOnce(e);
            local.reset(key);
        }
    }

    private void warnOnce(Exception e) {
        long now = System.currentTimeMillis();
        long last = lastWarnMillis.get();
        if (now - last > 60_000 && lastWarnMillis.compareAndSet(last, now)) {
            log.warn("Redis 限流不可用，已降级为本地计数: {}", e.getMessage());
        }
    }

    private static long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private static long ceilSeconds(long millis) {
        return Math.max(1, (millis + 999) / 1000);
    }
}
