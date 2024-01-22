package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotations.Experimental;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Operator;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * In bottom-up traversal, if we traverse all the branches in the tree,
 * we will encounter some nodes twice. This happens because 2 or more
 * leaf nodes may share the same parent.
 * Fortunately we don't need to traverse all the branches in the tree.
 * We stop after the current branch once we find a match.
 * @param <K>
 */
@Experimental
final class RateLimiterCompositeBottomUp<K> implements RateLimiter {
    private final K key;
    private final Node<RateContext<K>>[] leafNodes;
    private final RateLimiterProvider rateLimiterProvider;

    RateLimiterCompositeBottomUp (K key,
            Node<RateContext<K>> rootNode,
            RateLimiterProvider rateLimiterProvider) {
        this.key = Objects.requireNonNull(key);
        this.leafNodes = collectLeafs(rootNode);
        this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
    }
    private static <R> Node<RateContext<R>> [] collectLeafs(Node<RateContext<R>> node) {
        Set<Node<RateContext<R>>> leafNodes = new LinkedHashSet<>();
        Predicate<Node<RateContext<R>>> test = n -> n.isLeaf() && n.hasValue();
        node.getRoot().visitAll(test, leafNodes::add);
        return leafNodes.toArray(new Node[0]);
    }

    @Override
    public double acquire(int permits) {
        PermitAcquiringVisitor visitor = new PermitAcquiringVisitor(permits);
        visitNodesBottomUp(visitor);
        return visitor.getTotalTimeSpent();
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        PermitAttemptingVisitor visitor = new PermitAttemptingVisitor(permits, timeout, unit);
        visitNodesBottomUp(visitor);
        return visitor.isNoLimitExceeded();
    }

    @Override
    public Bandwidth getBandwidth() {
        List<Bandwidth> bandwidths = new ArrayList<>();
        BiConsumer<String, RateLimiter> visitor = (match, rateLimiter) -> {
            bandwidths.add(rateLimiter.getBandwidth());
        };
        visitNodesBottomUp(visitor);
        // For multiple Bandwidths conjugated with Operator.OR, the composed Bandwidth
        // succeeds only when all Bandwidths succeed. This is the case here.
        return Bandwidths.of(Operator.OR, bandwidths.toArray(new Bandwidth[0]));
    }

    private void visitNodesBottomUp(BiConsumer<String, RateLimiter> visitor) {
        for (Node<RateContext<K>> node : leafNodes) {
            boolean atLeastOneNodeInBranchMatched = false;
            do {
                // We need to traverse the entire current branch, even if we find a match.
                // However, we stop at the current branch, if we find a match in it.
                if (matchesRateLimiters(node, visitor)) {
                    atLeastOneNodeInBranchMatched = true;
                }
                node = node.getParentOrDefault(null);
            } while(node != null);
            // If at least one node in the last branch matches, we skip
            // the subsequent branches
            if (atLeastOneNodeInBranchMatched) {
                break;
            }
        }
    }

    private boolean matchesRateLimiters(
            Node<RateContext<K>> node, BiConsumer<String, RateLimiter> visitor) {
        return MatchUtil.matchesRateLimiters(node, key, rateLimiterProvider, visitor);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append("RateLimiterCompositeBottomUp@")
                .append(Integer.toHexString(hashCode()))
                .append("{");
        BiConsumer<String, RateLimiter> visitor = (match, rateLimiter) -> {
            builder.append("\n\tmatch=").append(match).append(", limiter=").append(rateLimiter);
        };
        visitNodesBottomUp(visitor);
        builder.append("\n}");
        return builder.toString();
    }
}
