package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.LimiterContext;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.Operator;

import java.util.Optional;
import java.util.function.Function;

public interface RateLimiterFactory<K> {

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
        return of(rateLimiterProvider, limiterNode);
    }

    static <K> RateLimiterFactory<K> of(
            RateLimiterProvider<String> rateLimiterProvider,
            Node<LimiterContext<K>> rootNode) {
        return new DefaultRateLimiterFactory<>(new RateLimiterTree<>(rateLimiterProvider, rootNode));
    }

    default RateLimiter getRateLimiterOrDefault(K key) {
        return getRateLimiter(key).orElse(RateLimiter.NO_LIMIT);
    }

    Optional<RateLimiter> getRateLimiter(K key);
}
