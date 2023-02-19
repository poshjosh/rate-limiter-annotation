package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.LimiterConfig;

// This is an internal interface
interface LimiterProvider<R, K> {

    static <R, K> LimiterProvider<R, K> ofDefaults() {
        return of(BandwidthsStore.ofDefaults());
    }

    static <R, K> LimiterProvider<R, K> of(BandwidthsStore<K> store) {
        return new DefaultLimiterProvider<>(store);
    }

    default RateLimiter getOrCreateLimiter(K key, LimiterConfig<R> limiterConfig) {
        return getOrCreateLimiter(key, limiterConfig, 0);
    }

    RateLimiter getOrCreateLimiter(K key, LimiterConfig<R> limiterConfig, int index);
}
