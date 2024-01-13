package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.LimiterContext;
import io.github.poshjosh.ratelimiter.util.Matcher;

import java.util.*;
import java.util.function.Predicate;

/**
 * A tree of {@link RateLimiter}s.
 * Provides means of visiting a hierarchy of {@link RateLimiter}s from leaf nodes upwards to root.
 * @param <K>
 */
final class RateLimiterTree<K> {

    interface RateLimiterConsumer {
        void accept(String match, RateLimiter rateLimiter,
                RateConfig config, int index, int max);
    }

    private final Node<LimiterContext<K>> rootNode;
    private final RateLimiterProvider<String> rateLimiterProvider;
    private final Collection<Node<LimiterContext<K>>> leafNodes;

    RateLimiterTree(
            Node<LimiterContext<K>> rootNode,
            RateLimiterProvider<String> rateLimiterProvider) {
        this.rootNode = Objects.requireNonNull(rootNode);
        this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
        this.leafNodes = collectLeafs(rootNode);
    }
    private static <R> Collection<Node<LimiterContext<R>>> collectLeafs(Node<LimiterContext<R>> node) {
        Set<Node<LimiterContext<R>>> leafNodes = new LinkedHashSet<>();
        Predicate<Node<LimiterContext<R>>> test = n -> n.isLeaf() && n.hasValue();
        node.getRoot().visitAll(test, leafNodes::add);
        return leafNodes;
    }

    /**
     * Visit all {@link RateLimiter}s in the tree matching the key, from leaf nodes upwards to root.
     * @param key The key to match
     * @param visitor The visitor to apply
     */
    public void visitRateLimiters(K key, RateLimiterConsumer visitor) {
        // We use this to avoid duplicates.
        final Set<String> visited = new HashSet<>();

        for (Node<LimiterContext<K>> node : leafNodes) {
            Node<LimiterContext<K>> current = node;
            do {

                final Node<LimiterContext<K>> constant = current;

                acceptMatchingNode(key, current, (match, index, total) -> {

                    LimiterContext<K> context = constant.requireValue();

                    if (visited.add(match)) {

                        RateLimiter limiter = getRateLimiter(context, match, index);

                        visitor.accept(match, limiter, context.getRateConfig(),
                                visited.size() - 1, total);
                    }
                });

                current = current.getParentOrDefault(null);

            } while(current != null);
        }
    }
    private void acceptMatchingNode(K key, Node<LimiterContext<K>> node, MatchConsumer visitor) {
        final LimiterContext<K> context = getLimiterContextOrNull(node);
        if (context == null) {
            return;
        }
        final String mainMatch = MatchUtil.match(node, key);
        if (context.hasSubConditions()) {
            final int count = context.getSubMatchers().size();
            for(int i = 0; i < count; i++) {
                final String match = MatchUtil.matchAt(node, key, i, mainMatch);
                if (Matcher.isMatch(match)) {
                    visitor.accept(match, i, count);
                }
            }
        } else {
            if (Matcher.isMatch(mainMatch)) {
                visitor.accept(mainMatch, -1, 1);
            }
        }
    }

    private RateLimiter getRateLimiter(LimiterContext<K> context, String match, int index) {
        if (index == -1) {
            return rateLimiterProvider.getRateLimiter(match, context.getRates());
        } else {
            return rateLimiterProvider.getRateLimiter(match, context.getRate(index));
        }
    }

    private LimiterContext<K> getLimiterContextOrNull(Node<LimiterContext<K>> node) {
        if (node == null) {
            return null;
        }
        return node.getValueOrDefault(null);
    }

    @Override
    public String toString() {
        return "RateLimiterTree{rootNode=" + rootNode + '}';
    }

    private interface MatchConsumer {
        void accept(String match, int index, int total);
    }
}
