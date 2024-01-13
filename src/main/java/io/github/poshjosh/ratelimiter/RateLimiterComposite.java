package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.BandwidthState;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

class RateLimiterComposite implements RateLimiter {

    private final RateLimiter[] rateLimiters;

    RateLimiterComposite(RateLimiter[] rateLimiters) {
        this.rateLimiters = Objects.requireNonNull(rateLimiters);
    }

    @Override
    public BandwidthState getBandwidth() {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double acquire(int permits) {
        double totalTime = 0;
        for (RateLimiter rateLimiter : rateLimiters) {
            final double timeSpent = rateLimiter.acquire(permits);
            if (timeSpent > 0) {
                totalTime += timeSpent;
            }
        }
        return totalTime;
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        int successCount = 0;
        for (RateLimiter rateLimiter : rateLimiters) {
            // We need to call all tryAcquire methods to ensure that the permits are reserved
            if (rateLimiter.tryAcquire(permits, timeout, unit)) {
                ++successCount;
            }
        }
        return successCount == rateLimiters.length;
    }
}
