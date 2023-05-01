package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.LimiterConfig;

public interface RateLimiterProvider<R, K> {

    static <R, K> RateLimiterProvider<R, K> ofDefaults() {
        return of(BandwidthsStore.ofDefaults());
    }

    static <R, K> RateLimiterProvider<R, K> of(BandwidthsStore<K> store) {
        return new DefaultRateLimiterProvider<>(store);
    }

    default RateLimiter getRateLimiter(K key, LimiterConfig<R> limiterConfig) {
        return getRateLimiter(key, limiterConfig, 0);
    }

    RateLimiter getRateLimiter(K key, LimiterConfig<R> limiterConfig, int index);
}
