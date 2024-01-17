package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

final class DefaultRateLimiterFactory<K> implements RateLimiterFactory<K> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRateLimiterFactory.class);

    private final Node<LimiterContext<K>> rootNode;
    private final RateLimiterProvider rateLimiterProvider;
    private final Collection<Node<LimiterContext<K>>> leafNodes;

    DefaultRateLimiterFactory(
            Node<LimiterContext<K>> rootNode,
            RateLimiterProvider rateLimiterProvider) {
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
     * Collect all {@link RateLimiter}s in the tree matching the key, from leaf nodes upwards to root.
     * @param key The key to match
     */
    public RateLimiter getRateLimiterOrDefault(K key, RateLimiter resultIfNone) {
        // TODO - We could have cached the result of this method with the key.
        // First check that performance is really improved.
        // Then ensure that the cache will not grow indefinitely.
        // Also ensure that this method is idempotent. i.e the same key
        // will always return the same RateLimiter. This might not be
        // the case if the Key is HttpServletRequest and the RateLimiter
        // is returned based on some request related condition. In that
        // case, 2 different HttpServletRequests could result to the
        // same RateLimiter.

        // We use the keys of this Map to prevent duplicates by match
        List<RateLimiter> list = new ArrayList<>();
        for (Node<LimiterContext<K>> node : leafNodes) {
            do {
                collectRateLimiters(key, node, list);
                node = node.getParentOrDefault(null);
            } while(node != null);
        }
        if (list.isEmpty()) {
            return resultIfNone;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return RateLimiters.of(list.toArray(new RateLimiter[0]));
    }

    private void collectRateLimiters(
            K key, Node<LimiterContext<K>> node, List<RateLimiter> list) {
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
                    RateLimiter rateLimiter =
                            rateLimiterProvider.getRateLimiter(match, context.getRate(i));
                    list.add(rateLimiter);
                }
            }
        } else {
            if (Matcher.isMatch(mainMatch)) {
                RateLimiter rateLimiter =
                        rateLimiterProvider.getRateLimiter(mainMatch, context.getRates());
                list.add(rateLimiter);
            }
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
        return "DefaultRateLimiterFactory{rootNode=" + rootNode + '}';
    }
}
