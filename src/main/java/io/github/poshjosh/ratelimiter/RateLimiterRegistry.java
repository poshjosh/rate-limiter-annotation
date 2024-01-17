package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.AnnotationConverter;
import io.github.poshjosh.ratelimiter.annotation.ElementId;

import java.lang.reflect.Method;
import java.util.Optional;

public interface RateLimiterRegistry<K> {

    static <K> RateLimiterRegistry<K> of(RateLimiterContext<K> context) {
        return new DefaultRateLimiterRegistry<>(
                context, RootNodes.of(context), AnnotationConverter.ofRate());
    }

    RateLimiterRegistry<K> register(Class<?> source);

    RateLimiterRegistry<K> register(Method source);

    RateLimiterFactory<K> createRateLimiterFactory();

    Optional<RateLimiter> getRateLimiter(Class<?> clazz);

    Optional<RateLimiter> getRateLimiter(Method method);


    default boolean isRegistered(Class<?> source) {
        return isRegistered(ElementId.of(source));
    }

    default boolean isRegistered(Method source) {
        return isRegistered(source.getDeclaringClass()) || isRegistered(ElementId.of(source));
    }

    boolean isRegistered(String name);
}
