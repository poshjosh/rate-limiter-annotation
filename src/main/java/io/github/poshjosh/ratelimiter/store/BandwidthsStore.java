package io.github.poshjosh.ratelimiter.store;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;

import java.util.WeakHashMap;

public interface BandwidthsStore<K> {

    static <K> BandwidthsStore<K> ofDefaults() {
        return new BandwidthsStoreOfMap<>(new WeakHashMap<>());
    }

    Bandwidth get(K key);
    void put(K key, Bandwidth bandwidth);
}
