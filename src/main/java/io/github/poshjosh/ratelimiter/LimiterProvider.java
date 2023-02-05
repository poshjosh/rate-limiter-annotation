package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.LimiterConfig;

import javax.cache.Cache;
import java.util.List;
import java.util.Map;

// This is an internal interface
interface LimiterProvider<R, K> {

    static <R, K> LimiterProvider<R, K> ofDefaults() {
        return of(BandwidthsStore.ofDefaults());
    }

    static <R, K> LimiterProvider<R, K> ofCache(Cache<K, Bandwidth[]> cache) {
        return of(BandwidthsStore.ofCache(cache));
    }

    static <R, K> LimiterProvider<R, K> ofMap(Map<K, Bandwidth[]> map) {
        return of(BandwidthsStore.ofMap(map));
    }

    static <R, K> LimiterProvider<R, K> of(BandwidthsStore<K> store) {
        return new DefaultLimiterProvider<>(store);
    }

    List<RateLimiter> getOrCreateLimiters(K key, LimiterConfig<R> rateConfig);
}
