package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.annotations.Beta;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.LimiterContext;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.Operator;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public interface RateLimiterFactory<K> {

    RateLimiterFactory<Object> NO_OP = new RateLimiterFactory<Object>() {
        @Override
        public RateLimiter getRateLimiterOrDefault(Object key, RateLimiter resultIfNone) {
            return resultIfNone;
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
    
    @Beta
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
        return of(RateProcessor.ofDefaults().processAll(sourceOfRateLimitInfo));
    }

    static <K> RateLimiterFactory<K> of(String resourceId, Rate limit) {
        return of(resourceId, RateConfig.of(RateSource.of(resourceId, limit != null), Rates.of(limit)));
    }

    static <K> RateLimiterFactory<K> of(String resourceId, Operator operator, Rate... limits) {
        final boolean hasLimits = limits != null && limits.length > 0;
        return of(resourceId, RateConfig.of(RateSource.of(resourceId, hasLimits),
                Rates.of(operator, limits)));
    }

    static <K> RateLimiterFactory<K> of(String resourceId, RateConfig rateConfig) {
        return of(Node.of(resourceId, rateConfig, Node.ofDefaultRoot()));
    }

    static <K> RateLimiterFactory<K> of(Node<RateConfig> node) {
        return of(RateLimiterProvider.ofDefaults(), MatcherProvider.ofDefaults(), node);
    }

    static <K> RateLimiterFactory<K> of(
            RateLimiterProvider<String> rateLimiterProvider,
            MatcherProvider<K> matcherProvider,
            Node<RateConfig> rootNode) {
        Function<Node<RateConfig>, LimiterContext<K>> transformer = currentNode -> {
            return LimiterContext.of(matcherProvider, currentNode);
        };
        Node<LimiterContext<K>> limiterNode = rootNode.getRoot().transform(transformer);
        return of(limiterNode, rateLimiterProvider);
    }

    static <K> RateLimiterFactory<K> of(
            Node<LimiterContext<K>> rootNode,
            RateLimiterProvider<String> rateLimiterProvider) {
        return new DefaultRateLimiterFactory<>(
                new RateLimiterTree<>(rootNode, rateLimiterProvider));
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
