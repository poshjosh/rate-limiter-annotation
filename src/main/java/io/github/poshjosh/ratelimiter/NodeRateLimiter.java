package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.matcher.MatchContext;
import io.github.poshjosh.ratelimiter.matcher.MatchContexts;
import io.github.poshjosh.ratelimiter.matcher.MatchVisitor;
import io.github.poshjosh.ratelimiter.matcher.MatchVisitors;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Ticker;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

class NodeRateLimiter<K> implements RateLimiter {

    static <K> boolean isWithinLimit(
            RateLimiterProvider rateLimiterProvider, Node<MatchContext<K>> rootNode,
            K key, Ticker ticker) {
        final MatchVisitor<Boolean> visitor =
                MatchVisitors.limitChecking(rateLimiterProvider, ticker);
        MatchContexts.visitNodes(rootNode, key, visitor);
        return visitor.getResult();
    }

    static <K> double acquire(
            RateLimiterProvider rateLimiterProvider, Node<MatchContext<K>> rootNode,
            K key, int permits) {
        final MatchVisitor<Double> visitor =
                MatchVisitors.permitAcquiring(rateLimiterProvider, permits);
        MatchContexts.visitNodes(rootNode, key, visitor);
        return visitor.getResult();
    }

    static <K> boolean tryAcquire(
            RateLimiterProvider rateLimiterProvider, Node<MatchContext<K>> rootNode,
            K key, int permits, long timeout, TimeUnit timeUnit) {
        final MatchVisitor<Boolean> visitor =
                MatchVisitors.permitAttempting(rateLimiterProvider, permits, timeout, timeUnit);
        MatchContexts.visitNodes(rootNode, key, visitor);
        return visitor.getResult();
    }


    private final K key;
    private final Node<MatchContext<K>> rootNode;
    private final RateLimiterProvider rateLimiterProvider;

    NodeRateLimiter(K key, Node<MatchContext<K>> rootNode, RateLimiterProvider rateLimiterProvider) {
        this.key = Objects.requireNonNull(key);
        this.rootNode = Objects.requireNonNull(rootNode);
        this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
    }

    @Override
    public Bandwidth getBandwidth() {
        final MatchVisitor<Bandwidth> visitor =
                MatchVisitors.bandwidthCollecting(rateLimiterProvider);
        MatchContexts.visitNodes(rootNode, key, visitor);
        return visitor.getResult();
    }

    @Override
    public double acquire(int permits) {
        return acquire(rateLimiterProvider, rootNode, key, permits);
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        return tryAcquire(rateLimiterProvider, rootNode, key, permits, timeout, unit);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(256)
                .append(this.getClass().getSimpleName())
                .append('@').append(Integer.toHexString(hashCode()))
                .append('{').append("key=").append(key);
        final int lengthBeforeVisit = builder.length();
        final MatchVisitor<StringBuilder> visitor =
                new MatchVisitors.MatchingRateLimiterVisitor<StringBuilder>(rateLimiterProvider) {
                    @Override
                    protected void visit(String match, RateLimiter rateLimiter) {
                        builder.append("\n\tmatch=").append(match).append(", limiter=").append(rateLimiter);
                    }
                };
        MatchContexts.visitNodes(rootNode, key, visitor);
        if (builder.length() > lengthBeforeVisit) {
            builder.append('\n');
        }
        builder.append('}');
        return builder.toString();
    }
}
