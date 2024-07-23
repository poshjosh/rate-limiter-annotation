package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.node.Node;

import java.util.*;
import java.util.function.Predicate;

final class DefaultRateLimiterFactory<K> implements RateLimiterFactory<K> {
    private final Node<RateContext<K>> rootNode;
    private final Node<RateContext<K>>[] leafNodes;
    private final RateLimiterProvider rateLimiterProvider;
    private final Map<K, RateLimiter> keyToRateLimiterMap;

    DefaultRateLimiterFactory(
            Node<RateContext<K>> rootNode,
            RateLimiterProvider rateLimiterProvider) {
        this.rootNode = Objects.requireNonNull(rootNode);
        this.leafNodes = RateContext.IS_BOTTOM_UP_TRAVERSAL ? collectLeafs(rootNode) : null;
        this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
        this.keyToRateLimiterMap = new WeakHashMap<>();
    }
    private static <R> Node<RateContext<R>> [] collectLeafs(Node<RateContext<R>> node) {
        Set<Node<RateContext<R>>> leafNodes = new LinkedHashSet<>();
        Predicate<Node<RateContext<R>>> test = n -> n.isLeaf() && n.hasValue();
        node.getRoot().visitAll(test, leafNodes::add);
        return leafNodes.toArray(new Node[0]);
    }

    /**
     * Collect all {@link RateLimiter}s in the tree matching the key, from leaf nodes upwards to root.
     * @param key The key to match
     */
    @Override
    public RateLimiter getRateLimiter(K key) {
        final RateLimiter fromCache = keyToRateLimiterMap.get(key);
        if (fromCache != null) {
            return fromCache;
        }
        final RateLimiter rateLimiter = createRateLimiter(key);
        keyToRateLimiterMap.put(key, rateLimiter);
        return rateLimiter;
    }

    @Override
    public RateLimiter createRateLimiter(K key) {
        return RateContext.IS_BOTTOM_UP_TRAVERSAL ?
                new RateLimiterCompositeBottomUp<>(key, leafNodes, rateLimiterProvider) :
                new RateLimiterComposite<>(key, rootNode, rateLimiterProvider);
    }

    @Override
    public String toString() {
        return "DefaultRateLimiterFactory{rootNode=" + rootNode + '}';
    }
}
