package io.github.poshjosh.ratelimiter;

import java.util.function.BiConsumer;

final class PermitAcquiringVisitor implements BiConsumer<String, RateLimiter> {
    private final int permits;

    private double totalTimeSpent = 0;

    PermitAcquiringVisitor(int permits) {
        this.permits = permits;
    }

    @Override public void accept(String match, RateLimiter rateLimiter) {
        double timeSpent = rateLimiter.acquire(permits);
        if (timeSpent > 0) { // Only increment when > 0, as some value may be negative.
            totalTimeSpent += timeSpent;
        }
    }

    public double getTotalTimeSpent() {
        return totalTimeSpent;
    }
}
