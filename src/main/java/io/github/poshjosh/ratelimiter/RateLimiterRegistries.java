package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.AnnotationConverter;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;

import java.util.Collections;

public interface RateLimiterRegistries {
    static RateLimiter getLimiter(Class<?> aClass) {
        return getLimiter(aClass, aClass);
    }

    static RateLimiter getLimiter(Class<?> aClass, Object id) {
        return of(aClass).getRateLimiter(id);
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
        RateSource rateSource = RateSource.of(resourceId, limit != null);
        return of(resourceId, RateConfig.of(rateSource, Rates.of(limit)));
    }

    static <K> RateLimiterRegistry<K> of(String resourceId, Operator operator, Rate... limits) {
        final boolean hasLimits = limits != null && limits.length > 0;
        return of(resourceId, RateConfig.of(RateSource.of(resourceId, hasLimits),
                Rates.of(operator, limits)));
    }

    static <K> RateLimiterRegistry<K> of(String resourceId, RateConfig rateConfig) {
        RateLimiterContext<K> context = RateLimiterContext.<K>builder()
                .rates(Collections.singletonMap(resourceId, rateConfig.getRates()))
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
        return new DefaultRateLimiterRegistry<>(
                context, RootNodes.of(context), AnnotationConverter.ofDefaults());
    }
}
