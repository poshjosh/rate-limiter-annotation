package io.github.poshjosh.ratelimiter.store;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;

import javax.cache.Cache;
import java.util.Objects;

final class BandwidthsStoreOfCache<K> implements BandwidthsStore<K>{

    private final Cache<K, Bandwidths> cache;

    BandwidthsStoreOfCache(Cache<K, Bandwidths> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    @Override public Bandwidths get(K key) {
        return cache.get(key);
    }

    @Override public void put(K key, Bandwidths bandwidths) {
        cache.put(key, bandwidths);
    }
}
