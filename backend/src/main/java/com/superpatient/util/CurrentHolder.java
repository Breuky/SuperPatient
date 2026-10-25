package com.superpatient.util;

/**
 * 线程上下文 — 持有当前登录用户的 ID 与原始 token。
 * token 只用于内部转发给 Agent 服务（工具调用复用用户权限），不返回前端。
 */
public class CurrentHolder {

    private static final ThreadLocal<Long> CURRENT_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TOKEN = new ThreadLocal<>();

    public static void setCurrentId(Long userId) {
        CURRENT_LOCAL.set(userId);
    }

    public static Long getCurrentId() {
        return CURRENT_LOCAL.get();
    }

    public static void setToken(String token) {
        CURRENT_TOKEN.set(token);
    }

    public static String getToken() {
        return CURRENT_TOKEN.get();
    }

    public static void remove() {
        CURRENT_LOCAL.remove();
        CURRENT_TOKEN.remove();
    }
}
