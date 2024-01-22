package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Operator;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

class RateLimiterComposite<K> implements RateLimiter {
    private final K key;
    private final Node<RateContext<K>> rootNode;
    private final RateLimiterProvider rateLimiterProvider;

    RateLimiterComposite (K key,
            Node<RateContext<K>> rootNode,
            RateLimiterProvider rateLimiterProvider) {
        this.key = Objects.requireNonNull(key);
        this.rootNode = Objects.requireNonNull(rootNode);
        this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
    }

    @Override
    public double acquire(int permits) {
        PermitAcquiringVisitor visitor = new PermitAcquiringVisitor(permits);
        visitNodesTopDown(visitor);
        return visitor.getTotalTimeSpent();
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        PermitAttemptingVisitor visitor = new PermitAttemptingVisitor(permits, timeout, unit);
        visitNodesTopDown(visitor);
        return visitor.isNoLimitExceeded();
    }

    @Override
    public Bandwidth getBandwidth() {
        List<Bandwidth> bandwidths = new ArrayList<>();
        BiConsumer<String, RateLimiter> visitor = (match, rateLimiter) -> {
            bandwidths.add(rateLimiter.getBandwidth());
        };
        visitNodesTopDown(visitor);
        // For multiple Bandwidths conjugated with Operator.OR, the composed Bandwidth
        // succeeds only when all Bandwidths succeed. This is the case here.
        return Bandwidths.of(Operator.OR, bandwidths.toArray(new Bandwidth[0]));
    }

    private void visitNodesTopDown(BiConsumer<String, RateLimiter> visitor) {
        AtomicBoolean matchFound = new AtomicBoolean(false);
        AtomicBoolean firstLeafAfterMatch = new AtomicBoolean(false);
        rootNode.visitAll(
                node -> !firstLeafAfterMatch.get(),
                node -> {
                    // We still need to traverse the entire current branch, even if we find a match
                    // However, we stop at the current branch, if we find a match in it.
                    if (matchesRateLimiters(node, visitor)) {
                        matchFound.set(true);
                    }
                    if (matchFound.get() && node.isLeaf()) {
                        firstLeafAfterMatch.set(true);
                    }
                });
    }

    private boolean matchesRateLimiters(
            Node<RateContext<K>> node, BiConsumer<String, RateLimiter> visitor) {
        return MatchUtil.matchesRateLimiters(node, key, rateLimiterProvider, visitor);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append("RateLimiterComposite@")
                .append(Integer.toHexString(hashCode()))
                .append("{");
        BiConsumer<String, RateLimiter> visitor = (match, rateLimiter) -> {
            builder.append("\n\tmatch=").append(match).append(", limiter=").append(rateLimiter);
        };
        visitNodesTopDown(visitor);
        builder.append("\n}");
        return builder.toString();
    }
}
