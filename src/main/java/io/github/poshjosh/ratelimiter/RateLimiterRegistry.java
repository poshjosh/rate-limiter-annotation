package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateId;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.Rates;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface RateLimiterRegistry<K> {

    interface Listener {
        default void onRateAdded(RateConfig rateConfig) { }
        default void onRateRemoved(RateConfig rateConfig) { }
    }

    void addListener(Listener listener);

    boolean isWithinLimit(K key);
    default boolean tryAcquire(K key, int permits) {
        return tryAcquire(key, permits, 0, TimeUnit.MICROSECONDS);
    }
    boolean tryAcquire(K key, int permits, long timeout, TimeUnit timeUnit);

    default RateLimiterRegistry<K> deregister(Class<?> source) {
        return deregister(RateId.of(source));
    }

    default RateLimiterRegistry<K> deregister(Method source) {
        return deregister(RateId.of(source));
    }

    RateLimiterRegistry<K> deregister(String id);

    default RateLimiterRegistry<K> register(String id, Rate rate) {
        return register(id, Rates.of(rate));
    }

    RateLimiterRegistry<K> register(String id, Rates rates);

    RateLimiterRegistry<K> register(Class<?> source);

    RateLimiterRegistry<K> register(Method source);

    default RateLimiter getRateLimiterOrUnlimited(K key) {
        return getRateLimiterOptional(key).orElse(RateLimiters.NO_LIMIT);
    }

    default RateLimiter getRateLimiter(K key) {
        return getRateLimiterOptional(key).orElseThrow(
                () -> new IllegalArgumentException("No rate limiter for " + key));
    }

    default RateLimiter getClassRateLimiter(Class<?> clazz) {
        return getClassRateLimiterOptional(clazz).orElseThrow(
                () -> new IllegalArgumentException("No rate limiter for " + clazz));
    }

    default RateLimiter getMethodRateLimiter(Method method) {
        return getMethodRateLimiterOptional(method).orElseThrow(
                () -> new IllegalArgumentException("No rate limiter for " + method));
    }

    Optional<RateLimiter> getRateLimiterOptional(K key);

    Optional<RateLimiter> getClassRateLimiterOptional(Class<?> clazz);

    Optional<RateLimiter> getMethodRateLimiterOptional(Method method);


    default boolean isRegistered(Class<?> source) {
        return isRegistered(RateId.of(source));
    }

    default boolean isRegistered(Method source) {
        return isRegistered(source.getDeclaringClass()) || isRegistered(RateId.of(source));
    }

    boolean isRegistered(String name);

    default boolean hasMatcher(Class<?> source) {
        return hasMatcher(RateId.of(source));
    }

    default boolean hasMatcher(Method source) {
        return hasMatcher(source.getDeclaringClass()) || hasMatcher(RateId.of(source));
    }

    boolean hasMatcher(String id);
}
