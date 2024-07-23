package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Operator;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * In bottom-up traversal, if we traverse all the branches in the tree,
 * we will encounter some nodes twice. This happens because 2 or more
 * leaf nodes may share the same parent.
 * Fortunately we don't need to traverse all the branches in the tree.
 * We stop after the current branch once we find a match.
 * @param <K>
 */
final class RateLimiterCompositeBottomUp<K> implements RateLimiter {
    private final K key;
    private final Node<RateContext<K>>[] leafNodes;
    private final RateLimiterProvider rateLimiterProvider;

    RateLimiterCompositeBottomUp (K key,
            Node<RateContext<K>>[] leafNodes,
            RateLimiterProvider rateLimiterProvider) {
        this.key = Objects.requireNonNull(key);
        this.leafNodes = Objects.requireNonNull(leafNodes);
        this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
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
        BiConsumer<String, RateLimiter> visitor = (match, rateLimiter) ->
                bandwidths.add(rateLimiter.getBandwidth());
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
        final StringBuilder builder = new StringBuilder()
                .append("RateLimiterCompositeBottomUp@")
                .append(Integer.toHexString(hashCode()))
                .append("{");
        final int lengthBeforeVisit = builder.length();
        BiConsumer<String, RateLimiter> visitor = (match, rateLimiter) ->
                builder.append("\n\tmatch=").append(match).append(", limiter=").append(rateLimiter);
        visitNodesBottomUp(visitor);
        if (builder.length() > lengthBeforeVisit) {
            builder.append('\n');
        }
        builder.append("}");
        return builder.toString();
    }
}
