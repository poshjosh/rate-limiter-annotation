package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.LimiterContext;

public interface RateLimiterProvider<R, K> {

    static <R, K> RateLimiterProvider<R, K> ofDefaults() {
        return of(BandwidthsStore.ofDefaults());
    }

    static <R, K> RateLimiterProvider<R, K> of(BandwidthsStore<K> store) {
        return new DefaultRateLimiterProvider<>(store);
    }

    default RateLimiter getRateLimiter(K key, LimiterContext<R> limiterContext) {
        return getRateLimiter(key, limiterContext, 0);
    }

    RateLimiter getRateLimiter(K key, LimiterContext<R> limiterContext, int index);
}
