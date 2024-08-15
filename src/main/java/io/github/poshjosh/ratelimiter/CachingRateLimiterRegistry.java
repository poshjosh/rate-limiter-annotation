package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.RateSource;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

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

    @Override public void addListener(Listener listener) {
        delegate.addListener(listener);
    }

    @Override public boolean isWithinLimit(K key) {
        return delegate.isWithinLimit(key);
    }

    @Override public boolean tryAcquire(K key, int permits, long timeout, TimeUnit timeUnit) {
        return delegate.tryAcquire(key, permits, timeout, timeUnit);
    }

    @Override public RateLimiterRegistry<K> deregister(String id) {
        return delegate.deregister(id);
    }

    @Override public RateLimiterRegistry<K> register(RateSource rateSource) {
        return delegate.register(rateSource);
    }

    @Override public RateLimiter getRateLimiterOrDefault(K key, RateLimiter resultIfNone) {
        final RateLimiter fromCache = getRateLimiterFromCacheOrNull(key);
        if (fromCache != null) {
            return fromCache;
        }
        final RateLimiter rateLimiter = delegate.getRateLimiterOrDefault(key, null);
        if (rateLimiter != null) {
            addRateLimiterToCache(key, rateLimiter);
        }
        return rateLimiter == null ? resultIfNone : rateLimiter;
    }

    @Override public RateLimiter getRateLimiterOrDefault(
            RateSource rateSource, RateLimiter resultIfNone) {
        final String key = rateSource.getId();
        final RateLimiter fromCache = getRateLimiterFromCacheOrNull(key);
        if (fromCache != null) {
            return fromCache;
        }
        final RateLimiter rateLimiter = delegate.getRateLimiterOrDefault(rateSource, null);
        if (rateLimiter != null) {
            addRateLimiterToCache(key, rateLimiter);
        }
        return rateLimiter == null ? resultIfNone : rateLimiter;
    }

    @Override public boolean isRegistered(String name) {
        return delegate.isRegistered(name);
    }

    @Override public boolean hasMatcher(String id) {
        return delegate.hasMatcher(id);
    }
}
