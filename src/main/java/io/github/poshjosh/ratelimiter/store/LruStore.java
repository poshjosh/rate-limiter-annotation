package io.github.poshjosh.ratelimiter.store;

import java.util.LinkedHashMap;
import java.util.Map;

// TODO: Consider using a more efficient implementation if need.
//       For example, https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/ConcurrentLruCache.html
final class LruStore<K, V> implements Store<K, V> {

    private final Map<K, V> cache;

    LruStore(int capacity, float loadFactor) {
        cache = new LRUCache<>(capacity, loadFactor);
    }

    public V get(K key) {
        return cache.get(key);
    }

    public void put(K key, V matcher) {
        cache.put(key, matcher);
    }

    private static final class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;
        private LRUCache(int capacity, float loadFactor) {
            super(capacity, loadFactor, true);
            this.capacity = capacity;
        }
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }
}
