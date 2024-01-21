package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.node.Node;

import java.util.*;

final class DefaultRateLimiterFactory<K> implements RateLimiterFactory<K> {
    private final Node<RateContext<K>> rootNode;
    private final RateLimiterProvider rateLimiterProvider;

    DefaultRateLimiterFactory(
            Node<RateContext<K>> rootNode,
            RateLimiterProvider rateLimiterProvider) {
        this.rootNode = Objects.requireNonNull(rootNode);
        this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
    }

    /**
     * Collect all {@link RateLimiter}s in the tree matching the key, from leaf nodes upwards to root.
     * @param key The key to match
     */
    public RateLimiter getRateLimiterOrDefault(K key, RateLimiter resultIfNone) {
        // We could cache the result of this method, but it could be disastrous
        // We cannot tell how many keys will be presented. This means that
        // the size of our cache may be arbitrarily large.
        return RateContext.isBottomUpTraversal() ?
                new RateLimiterCompositeBottomUp<>(key, rootNode, rateLimiterProvider) :
                new RateLimiterComposite<>(key, rootNode, rateLimiterProvider);
    }

    @Override
    public String toString() {
        return "DefaultRateLimiterFactory{rootNode=" + rootNode + '}';
    }
}
