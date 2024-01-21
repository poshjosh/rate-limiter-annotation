package io.github.poshjosh.ratelimiter;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

class PermitAttemptingVisitor implements BiConsumer<String, RateLimiter> {
    private final int permits;

    private final long timeout;

    private final TimeUnit timeUnit;

    private boolean noLimitExceeded = true;

    PermitAttemptingVisitor(int permits, long timeout, TimeUnit timeUnit) {
        this.permits = permits;
        this.timeout = timeout;
        this.timeUnit = Objects.requireNonNull(timeUnit);
    }

    @Override public void accept(String match, RateLimiter rateLimiter) {
        if (!rateLimiter.tryAcquire(permits, timeout, timeUnit)) {
            noLimitExceeded = false;
        }
    }

    public boolean isNoLimitExceeded() {
        return noLimitExceeded;
    }
}
