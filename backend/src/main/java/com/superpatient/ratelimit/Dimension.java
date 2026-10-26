package com.superpatient.ratelimit;

/** 限流维度：按 IP 还是按登录用户 */
public enum Dimension {
    IP,
    USER
}
