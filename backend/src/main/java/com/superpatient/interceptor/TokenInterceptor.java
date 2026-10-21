package com.superpatient.interceptor;

import com.superpatient.util.CurrentHolder;
import com.superpatient.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 令牌拦截器
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    public TokenInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头获取 token
        String token = request.getHeader("token");

        // 2. 无 token → 401
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // 3. 解析 token，取出 userId 存入 ThreadLocal
        try {
            Claims claims = jwtUtils.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            CurrentHolder.setCurrentId(userId);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清除 ThreadLocal，防止内存泄漏
        CurrentHolder.remove();
    }
}
