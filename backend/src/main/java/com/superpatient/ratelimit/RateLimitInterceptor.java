package com.superpatient.ratelimit;

import com.superpatient.exception.BusinessException;
import com.superpatient.util.CurrentHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Set;

/**
 * 读取方法上的 @RateLimit（可多个），逐个检查。
 *
 * 注意 IP 取值：只有配置了 app.rate-limit.trust-proxy=true 才信 X-Forwarded-For，
 * 否则用 remoteAddr——否则攻击者换个请求头就能绕过。
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private final boolean trustProxy;

    public RateLimitInterceptor(RateLimiter rateLimiter,
                                @Value("${app.rate-limit.trust-proxy:false}") boolean trustProxy) {
        this.rateLimiter = rateLimiter;
        this.trustProxy = trustProxy;
        log.info("限流拦截器已注册（trust-proxy={}）", trustProxy);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        Set<RateLimit> annotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(
                method.getMethod(), RateLimit.class);
        log.debug("限流检查 {} -> {} 条规则", request.getRequestURI(), annotations.size());
        for (RateLimit rule : annotations) {
            String key = "rl:" + rule.scope() + ":" + rule.dimension().name().toLowerCase() + ":" + subject(rule.dimension(), request);
            Duration window = Duration.of(rule.window(), rule.unit().toChronoUnit());
            RateLimiter.Decision decision = rateLimiter.check(key, rule.limit(), window, rule.failOpen());
            if (!decision.allowed()) {
                throw new RateLimitExceededException(decision.retryAfterSeconds());
            }
        }
        return true;
    }

    private String subject(Dimension dimension, HttpServletRequest request) {
        if (dimension == Dimension.IP) {
            return clientIp(request);
        }
        Long userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }
        return String.valueOf(userId);
    }

    private String clientIp(HttpServletRequest request) {
        if (trustProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
