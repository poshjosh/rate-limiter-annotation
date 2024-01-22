package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.AnnotationConverter;
import io.github.poshjosh.ratelimiter.annotation.RateId;

import java.lang.reflect.Method;
import java.util.Optional;

public interface RateLimiterRegistry<K> {

    static <K> RateLimiterRegistry<K> of(RateLimiterContext<K> context) {
        return new DefaultRateLimiterRegistry<>(
                context, RootNodes.of(context), AnnotationConverter.ofDefaults());
    }

    RateLimiterRegistry<K> register(Class<?> source);

    RateLimiterRegistry<K> register(Method source);

    RateLimiterFactory<K> createRateLimiterFactory();

    Optional<RateLimiter> getRateLimiter(Class<?> clazz);

    Optional<RateLimiter> getRateLimiter(Method method);


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
