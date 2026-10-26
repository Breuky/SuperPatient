package com.superpatient.ratelimit;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 不可用时的兜底：进程内固定窗口计数。
 *
 * 单机够用，多实例不精确——所以它只是降级方案，正常路径走 Redis。
 * 固定窗口有边界突刺（窗口末尾和下一窗口开头各打满一次），
 * 兜底场景可以接受，不值得为此再做一套滑动窗口。
 */
class LocalWindowCounter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** @return 允许时返回 -1；拒绝时返回还需等待的毫秒数 */
    synchronized long hitAndRetryAfter(String key, int limit, Duration window, long nowMillis) {
        long windowMillis = window.toMillis();
        Window current = windows.get(key);

        if (current == null || nowMillis - current.startMillis >= windowMillis) {
            windows.put(key, new Window(nowMillis, 1));
            return -1;
        }
        if (current.count >= limit) {
            return windowMillis - (nowMillis - current.startMillis);
        }
        windows.put(key, new Window(current.startMillis, current.count + 1));
        return -1;
    }

    /** 测试与登出用 */
    synchronized void reset(String key) {
        windows.remove(key);
    }

    private record Window(long startMillis, int count) {
    }
}
