package io.github.poshjosh.ratelimiter.store;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;

public interface BandwidthsStore<K> extends Store <K, Bandwidth> {
    Bandwidth get(K key);
    void put(K key, Bandwidth bandwidth);
}
