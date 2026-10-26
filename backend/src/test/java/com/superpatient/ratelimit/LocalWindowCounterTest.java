package com.superpatient.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 不可用时的兜底：窗口内不超发、窗口过后恢复
 */
class LocalWindowCounterTest {

    private final LocalWindowCounter counter = new LocalWindowCounter();
    private final Duration window = Duration.ofMinutes(1);

    @Test
    void allowsUpToLimitThenDenies() {
        long now = 1_000_000L;
        for (int i = 0; i < 3; i++) {
            assertEquals(-1, counter.hitAndRetryAfter("k", 3, window, now), "第 " + (i + 1) + " 次应放行");
        }
        long retryAfter = counter.hitAndRetryAfter("k", 3, window, now + 10_000);
        assertTrue(retryAfter > 0, "第 4 次应拒绝，并给出等待时间");
        assertTrue(retryAfter <= window.toMillis());
    }

    @Test
    void resetsAfterWindow() {
        long now = 2_000_000L;
        for (int i = 0; i < 3; i++) {
            counter.hitAndRetryAfter("k2", 3, window, now);
        }
        assertEquals(-1, counter.hitAndRetryAfter("k2", 3, window, now + window.toMillis() + 1),
                "窗口过后应重新放行");
    }

    @Test
    void keysAreIndependent() {
        long now = 3_000_000L;
        for (int i = 0; i < 2; i++) {
            counter.hitAndRetryAfter("a", 2, window, now);
        }
        assertEquals(-1, counter.hitAndRetryAfter("b", 2, window, now), "另一个 key 不受影响");
    }

    @Test
    void concurrentRequestsNeverOverAdmit() throws Exception {
        int limit = 5;
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger allowed = new AtomicInteger();

        long now = 4_000_000L;
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (counter.hitAndRetryAfter("hot", limit, window, now) < 0) {
                        allowed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(limit, allowed.get(), "并发下也只能放行 limit 次");
    }
}
