package io.github.poshjosh.ratelimiter.store;

public interface Store<K, V> {
    V get(K key);
    void put(K key, V value);
}
