package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;

import java.util.*;

public interface RateLimiterFactory<K> {

    RateLimiterFactory<Object> NO_OP = new RateLimiterFactory<Object>() {
        @Override
        public RateLimiter getRateLimiterOrDefault(Object key, RateLimiter resultIfNone) {
            return resultIfNone;
        }
        @Override
        public RateLimiterFactory<Object> andThen(RateLimiterFactory<Object> after) {
            return after;
        }
        @Override
        public String toString() {
            return "RateLimiterFactory$NO_OP";
        }
    };

    @SuppressWarnings("unchecked")
    static <K> RateLimiterFactory<K> noop() {
        return (RateLimiterFactory<K>) NO_OP;
    }

    static RateLimiter getLimiter(Class<?> aClass) {
        return getLimiter(aClass, aClass);
    }

    static RateLimiter getLimiter(Class<?> aClass, Object id) {
        return RateLimiterFactory.of(aClass).getRateLimiter(id);
    }

    /**
     * Create a RateLimiterFactory for the specified classes
     * @param sourceOfRateLimitInfo The classes which contain rate limit related annotations.
     * @return A RateLimiterFactory instance.
     * @param <K> The type of the ID for each resource
     */
    static <K> RateLimiterFactory<K> of(Class<?>... sourceOfRateLimitInfo) {
        return of(RateLimiterContext.<K>builder().classes(sourceOfRateLimitInfo).build());
    }

    static <K> RateLimiterFactory<K> of(String resourceId, Rate limit) {
        RateSource rateSource = RateSource.of(resourceId, limit != null);
        return of(resourceId, RateConfig.of(rateSource, Rates.of(limit)));
    }

    static <K> RateLimiterFactory<K> of(String resourceId, Operator operator, Rate... limits) {
        final boolean hasLimits = limits != null && limits.length > 0;
        return of(resourceId, RateConfig.of(RateSource.of(resourceId, hasLimits),
                Rates.of(operator, limits)));
    }

    static <K> RateLimiterFactory<K> of(String resourceId, RateConfig rateConfig) {
        RateLimiterContext<K> context = RateLimiterContext.<K>builder()
                .rates(Collections.singletonMap(resourceId, rateConfig.getRates()))
                .build();
        return of(context);
    }

    static <K> RateLimiterFactory<K> of(RateLimitProperties properties) {
        RateLimiterContext<K> context = RateLimiterContext.<K>builder()
                .properties(properties)
                .build();
        return RateLimiterRegistry.of(context).createRateLimiterFactory();
    }

    static <K> RateLimiterFactory<K> of(RateLimiterContext<K> context) {
        return RateLimiterFactoryCreator.create(context);
    }

    default RateLimiter getRateLimiter(K key) {
        return getRateLimiterOrDefault(key, RateLimiter.NO_LIMIT);
    }


    default Optional<RateLimiter> getRateLimiterOptional(K key) {
        return Optional.ofNullable(getRateLimiterOrDefault(key, null)); 
    }
    
    RateLimiter getRateLimiterOrDefault(K key, RateLimiter resultIfNone);
    
    default RateLimiterFactory<K> andThen(RateLimiterFactory<K> after) {
        Objects.requireNonNull(after);
        return new RateLimiterFactory<K>() {
            @Override 
            public RateLimiter getRateLimiterOrDefault(K key, RateLimiter resultIfNone) {
                final RateLimiter l = RateLimiterFactory.this.getRateLimiterOrDefault(key, resultIfNone);
                final RateLimiter r = after.getRateLimiterOrDefault(key, resultIfNone);
                return RateLimiters.of(l, r);
            }
            @Override 
            public String toString() {
                return "RateLimiterFactory$andThen{l=" + RateLimiterFactory.this + ", r=" + after + "}";
            }
        };
    }
}
