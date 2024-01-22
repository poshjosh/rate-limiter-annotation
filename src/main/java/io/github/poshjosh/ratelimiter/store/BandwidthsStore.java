package io.github.poshjosh.ratelimiter.store;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;

import java.util.Map;
import java.util.WeakHashMap;

public interface BandwidthsStore<K> {

    static <K> BandwidthsStore<K> ofDefaults() {
        return ofMap(new WeakHashMap<>());
    }

    static <K> BandwidthsStore<K> ofMap(Map<K, Bandwidth> map) {
        return new BandwidthsStoreOfMap<>(map);
    }

    Bandwidth get(K key);
    void put(K key, Bandwidth bandwidth);
}
