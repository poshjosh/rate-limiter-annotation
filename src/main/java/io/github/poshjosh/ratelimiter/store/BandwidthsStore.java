package io.github.poshjosh.ratelimiter.store;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;

import javax.cache.Cache;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface BandwidthsStore<K> {

    static <K> BandwidthsStore<K> ofDefaults() {
        return ofMap(new ConcurrentHashMap<>());
    }

    static <K> BandwidthsStore<K> ofCache(Cache<K, Bandwidths> cache) {
        return new BandwidthsStoreOfCache<>(cache);
    }

    static <K> BandwidthsStore<K> ofMap(Map<K, Bandwidths> map) {
        return new BandwidthsStoreOfMap<>(map);
    }

    Bandwidths get(K key);
    void put(K key, Bandwidths bandwidths);
}
