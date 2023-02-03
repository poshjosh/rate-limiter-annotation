package io.github.poshjosh.ratelimiter.store;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;

import java.util.Map;
import java.util.Objects;

final class BandwidthsStoreOfMap<K> implements BandwidthsStore<K>{

    private final Map<K, Bandwidth[]> map;

    BandwidthsStoreOfMap(Map<K, Bandwidth[]> map) {
        this.map = Objects.requireNonNull(map);
    }

    @Override public Bandwidth[] get(K key) {
        return map.get(key);
    }

    @Override public void put(K key, Bandwidth[] bandwidths) {
        map.put(key, bandwidths);
    }
}
