package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.JavaRateSources;
import io.github.poshjosh.ratelimiter.annotation.RateId;
import io.github.poshjosh.ratelimiter.model.*;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface RateLimiterRegistry<K> {

    boolean isWithinLimit(K key);
    default boolean tryAcquire(K key, int permits) {
        return tryAcquire(key, permits, 0, TimeUnit.MICROSECONDS);
    }
    boolean tryAcquire(K key, int permits, long timeout, TimeUnit timeUnit);

    Set<String> getRateNames();

    void visitRates(Consumer<MatchContext<K>> visitor);

    default Optional<MatchContext<K>> getMatchContextOptional(String id) {
        return Optional.ofNullable(getMatchContextOrDefault(id, null));
    }

    MatchContext<K> getMatchContextOrDefault(String id, MatchContext<K> resultIfNone);

    default RateLimiterRegistry<K> deregister(Class<?> source) {
        return deregister(RateId.of(source));
    }

    default RateLimiterRegistry<K> deregister(Method source) {
        return deregister(RateId.of(source));
    }

    RateLimiterRegistry<K> deregister(String id);

    default RateLimiterRegistry<K> register(String parentId, String id, Rate rate) {
        return register(new Rates().parentId(parentId).id(id).rates(rate));
    }

    default RateLimiterRegistry<K> register(Rates rates) {
        return register(RateSources.of(rates));
    }

    default RateLimiterRegistry<K> register(Class<?> source) {
        return register(JavaRateSources.of(source));
    }

    default RateLimiterRegistry<K> register(Method source) {
        return register(JavaRateSources.of(source));
    }

    RateLimiterRegistry<K> register(RateSource rateSource);

    default RateLimiter getRateLimiterOrUnlimited(K key) {
        return getRateLimiterOrDefault(key, RateLimiters.NO_LIMIT);
    }

    default RateLimiter requireRateLimiter(K key) {
        return getRateLimiterOptional(key).orElseThrow(
                () -> new IllegalArgumentException("No rate limiter for " + key));
    }

    default RateLimiter requireClassRateLimiter(Class<?> clazz) {
        return getRateLimiterOptional(JavaRateSources.of(clazz)).orElseThrow(
                () -> new IllegalArgumentException("No rate limiter for " + clazz));
    }

    default RateLimiter requireMethodRateLimiter(Method method) {
        return getRateLimiterOptional(JavaRateSources.of(method)).orElseThrow(
                () -> new IllegalArgumentException("No rate limiter for " + method));
    }

    default Optional<RateLimiter> getRateLimiterOptional(K key) {
        return Optional.ofNullable(getRateLimiterOrDefault(key, null));
    }
    RateLimiter getRateLimiterOrDefault(K key, RateLimiter resultIfNone);

    default Optional<RateLimiter> getRateLimiterOptional(Class<?> clazz) {
        return getRateLimiterOptional(JavaRateSources.of(clazz));
    }

    default Optional<RateLimiter> getRateLimiterOptional(Method method) {
        return getRateLimiterOptional(JavaRateSources.of(method));
    }

    default Optional<RateLimiter> getRateLimiterOptional(RateSource rateSource) {
        return Optional.ofNullable(getRateLimiterOrDefault(rateSource, null));
    }

    RateLimiter getRateLimiterOrDefault(RateSource rateSource, RateLimiter resultIfNone);


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
