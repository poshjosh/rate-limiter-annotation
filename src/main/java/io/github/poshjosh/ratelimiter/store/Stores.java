package io.github.poshjosh.ratelimiter.store;

import java.util.WeakHashMap;

public final class Stores {
    public static <K> BandwidthsStore<K> ofBandwidths() {
        return new BandwidthsStoreOfMap<>(new WeakHashMap<>());
    }
    public static <K, V> Store<K, V> ofLRU(int capacity, float loadFactor) {
        return new LruStore<>(capacity, loadFactor);
    }
    public static <K, V> Store<K, V> noop() {
        return new Store<K, V>() {
            @Override public V get(K key) { return null; }
            @Override public void put(K key, V value) {
                // Noop store
            }
            @Override public String toString() { return "Store.Noop{}"; }
        };
    }
    private Stores() { }
}
