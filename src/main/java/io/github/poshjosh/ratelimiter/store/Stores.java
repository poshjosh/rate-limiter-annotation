package io.github.poshjosh.ratelimiter.store;

import java.util.WeakHashMap;

public final class Stores {
    public static <K> BandwidthsStore<K> ofBandwidths() {
        return new BandwidthsStoreOfMap<>(new WeakHashMap<>());
    }
    public static <K, V> Store<K, V> ofLRU(int capacity, float loadFactor) {
        return new LruStore<>(capacity, loadFactor);
    }
    private Stores() { }
}
