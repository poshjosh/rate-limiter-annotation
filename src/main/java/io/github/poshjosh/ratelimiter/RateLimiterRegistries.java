package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.*;
import io.github.poshjosh.ratelimiter.model.Operator;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;

import java.util.Collections;

public interface RateLimiterRegistries {
    static RateLimiter getLimiter(Class<?> aClass) {
        return getLimiter(aClass, aClass);
    }

    static RateLimiter getLimiter(Class<?> aClass, Object id) {
        return of(aClass).requireRateLimiter(id);
    }

    /**
     * Create a RateLimiterRegistry for the specified classes
     * @param sourceOfRateLimitInfo The classes which contain rate limit related annotations.
     * @return A RateLimiterRegistry instance.
     * @param <K> The type of the ID for each resource
     */
    static <K> RateLimiterRegistry<K> of(Class<?>... sourceOfRateLimitInfo) {
        return of(RateLimiterContext.<K>builder().classes(sourceOfRateLimitInfo).build());
    }

    static <K> RateLimiterRegistry<K> of(String resourceId, Rate limit) {
        Rates rates = Rates.of(resourceId, limit);
        RateSource rateSource = RateSources.of(rates);
        return of(RateConfig.of(rateSource, rates));
    }

    static <K> RateLimiterRegistry<K> of(String resourceId, Operator operator, Rate... limits) {
        final Rates rates = Rates.of(resourceId, operator, "", limits);
        return of(RateConfig.of(RateSources.of(rates), rates));
    }

    static <K> RateLimiterRegistry<K> of(RateConfig rateConfig) {
        RateLimiterContext<K> context = RateLimiterContext.<K>builder()
                .rates(Collections.singletonList(rateConfig.getRates()))
                .build();
        return of(context);
    }

    static <K> RateLimiterRegistry<K> of(RateLimitProperties properties) {
        RateLimiterContext<K> context = RateLimiterContext.<K>builder()
                .properties(properties)
                .build();
        return of(context);
    }

    static <K> RateLimiterRegistry<K> of(RateLimiterContext<K> context) {
        return new DefaultRateLimiterRegistry<>(context, RootNodes.of(context));
    }

    static <K> RateLimiterRegistry<K> ofCaching(RateLimiterRegistry<K> registry) {
        return new CachingRateLimiterRegistry<>(registry);
    }
}
