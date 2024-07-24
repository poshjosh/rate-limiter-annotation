package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateId;

import java.lang.reflect.Method;
import java.util.Optional;

public interface RateLimiterRegistry<K> {

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
