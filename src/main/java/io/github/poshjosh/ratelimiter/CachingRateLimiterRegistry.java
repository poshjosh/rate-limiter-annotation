package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateId;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

final class CachingRateLimiterRegistry<K> implements RateLimiterRegistry<K> {
    private final RateLimiterRegistry<K> delegate;
    private Map<Object, RateLimiter> rateLimiterCache;

    CachingRateLimiterRegistry(RateLimiterRegistry<K> delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    private RateLimiter getRateLimiterFromCacheOrNull(Object key) {
        return rateLimiterCache == null ? null : rateLimiterCache.get(key);
    }
    private RateLimiter addRateLimiterToCache(Object key, RateLimiter rateLimiter) {
        if (rateLimiter == null) {
            return null;
        }
        if (rateLimiterCache == null) {
            rateLimiterCache = new WeakHashMap<>();
        }
        rateLimiterCache.put(key, rateLimiter);
        return rateLimiter;
    }

    @Override public RateLimiterRegistry<K> register(Class<?> source) {
        return delegate.register(source);
    }

    @Override public RateLimiterRegistry<K> register(Method source) {
        return delegate.register(source);
    }

    @Override public Optional<RateLimiter> getRateLimiterOptional(K key) {
        final RateLimiter fromCache = getRateLimiterFromCacheOrNull(key);
        if (fromCache != null) {
            return Optional.of(fromCache);
        }
        return delegate.getRateLimiterOptional(key)
                .map(rateLimiter -> addRateLimiterToCache(key, rateLimiter));
    }

    @Override public Optional<RateLimiter> getClassRateLimiterOptional(Class<?> clazz) {
        final RateLimiter fromCache = getRateLimiterFromCacheOrNull(clazz);
        if (fromCache != null) {
            return Optional.of(fromCache);
        }
        return delegate.getClassRateLimiterOptional(clazz)
                .map(rateLimiter -> addRateLimiterToCache(RateId.of(clazz), rateLimiter));
    }

    @Override public Optional<RateLimiter> getMethodRateLimiterOptional(Method method) {
        final RateLimiter fromCache = getRateLimiterFromCacheOrNull(method);
        if (fromCache != null) {
            return Optional.of(fromCache);
        }
        return delegate.getMethodRateLimiterOptional(method)
                .map(rateLimiter -> addRateLimiterToCache(RateId.of(method), rateLimiter));
    }

    @Override public boolean isRegistered(String name) {
        return delegate.isRegistered(name);
    }

    @Override public boolean hasMatcher(String id) {
        return delegate.hasMatcher(id);
    }
}
