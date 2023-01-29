package io.github.poshjosh.ratelimiter.store;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;

import java.util.Map;
import java.util.Objects;

final class BandwidthsStoreOfMap<K> implements BandwidthsStore<K>{

    private final Map<K, Bandwidths> map;

    BandwidthsStoreOfMap(Map<K, Bandwidths> map) {
        this.map = Objects.requireNonNull(map);
    }

    @Override public Bandwidths get(K key) {
        return map.get(key);
    }

    @Override public void put(K key, Bandwidths bandwidths) {
        map.put(key, bandwidths);
    }
}
