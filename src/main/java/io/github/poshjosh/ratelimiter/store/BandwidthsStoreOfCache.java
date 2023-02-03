package io.github.poshjosh.ratelimiter.store;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;

import javax.cache.Cache;
import java.util.Objects;

final class BandwidthsStoreOfCache<K> implements BandwidthsStore<K>{

    private final Cache<K, Bandwidth[]> cache;

    BandwidthsStoreOfCache(Cache<K, Bandwidth[]> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    @Override public Bandwidth[] get(K key) {
        return cache.get(key);
    }

    @Override public void put(K key, Bandwidth[] bandwidths) {
        cache.put(key, bandwidths);
    }
}
