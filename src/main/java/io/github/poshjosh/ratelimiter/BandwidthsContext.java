package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.RateConfig;

import javax.cache.Cache;
import java.util.Map;

interface BandwidthsContext<K> {

    static <K> BandwidthsContext<K> ofDefaults() {
        return of(SleepingTicker.zeroOffset(), BandwidthsStore.ofDefaults());
    }

    static <K> BandwidthsContext<K> of(SleepingTicker ticker) {
        return of(ticker, BandwidthsStore.ofDefaults());
    }

    static <K> BandwidthsContext<K> ofCache(SleepingTicker ticker, Cache<K, Bandwidths> cache) {
        return of(ticker, BandwidthsStore.ofCache(cache));
    }

    static <K> BandwidthsContext<K> ofMap(SleepingTicker ticker, Map<K, Bandwidths> map) {
        return of(ticker, BandwidthsStore.ofMap(map));
    }

    static <K> BandwidthsContext<K> of(SleepingTicker ticker, BandwidthsStore<K> store) {
        return new DefaultBandwidthsContext<>(ticker, store);
    }

    SleepingTicker getTicker(K key);

    Bandwidths getBandwidths(K key, RateConfig config);

    void onChange(K key, Bandwidths bandwidths);

    RateLimiter getLimiter(K key, Bandwidths bandwidths);
}
