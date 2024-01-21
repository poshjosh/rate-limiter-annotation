package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

class RateLimiterComposite<K> implements RateLimiter {
    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterComposite.class);
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
        rootNode.visitAll(node -> acceptMatchingRateLimiters(node, visitor));
        return visitor.totalTime;
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        PermitAttemptingVisitor visitor = new PermitAttemptingVisitor(permits, timeout, unit);
        rootNode.visitAll(node -> acceptMatchingRateLimiters(node, visitor));
        return visitor.noLimitExceeded;
    }

    @Override
    public Bandwidth getBandwidth() {
        List<Bandwidth> bandwidths = new ArrayList<>();
        rootNode.visitAll(node -> {
            acceptMatchingRateLimiters(node,
                    (match, rateLimiter) -> bandwidths.add(rateLimiter.getBandwidth()));
        });
        if (bandwidths.isEmpty()) {
            // We are unlimited if there is no bandwidth
            return Bandwidth.UNLIMITED;
        }
        if (bandwidths.size() == 1) {
            return bandwidths.get(0);
        }
        // For multiple Bandwidths conjugated with Operator.OR, the composed Bandwidth
        // succeeds only when all Bandwidths succeed. This is the case here.
        return Bandwidths.of(Operator.OR, bandwidths.toArray(new Bandwidth[0]));
    }

    protected int acceptMatchingRateLimiters(
            Node<RateContext<K>> node,
            BiConsumer<String, RateLimiter> visitor) {
        final RateContext<K> context = node == null ? null : node.getValueOrDefault(null);
        if (context == null) {
            return -1;
        }
        final String mainMatch = MatchUtil.match(node, key);
        if (context.hasSubConditions()) {
            final int count = context.getSubMatchers().size();
            int matchCount = 0;
            for(int i = 0; i < count; i++) {
                final String match = MatchUtil.matchAt(node, key, i, mainMatch);
                if (Matcher.isMatch(match)) {
                    ++matchCount;
                    RateLimiter rateLimiter =
                            rateLimiterProvider.getRateLimiter(match, context.getRate(i));

                    visitor.accept(match, rateLimiter);

                    if (LOG.isTraceEnabled()) {
                        LOG.trace("[{}]{} = {}", i, match, rateLimiter);
                    }
                }
            }
            return matchCount;
        } else {
            if (Matcher.isMatch(mainMatch)) {

                // We use parent rates as fallback. (Applies only to main matcher).
                //
                // This is useful for matchers which cannot match a rate source's parent.
                //
                // When a class or method is used as match candidate,
                // We can write a matcher like RateSourceMatcher which matches the candidate
                // with the rate source of the matcher and possibly the rate source's parent.
                // This is because we can use reflection to get a class or method's "parent".
                //
                // On the other hand, when an arbitrary value is used as match candidate,
                // we have to rely on other means. This fallback to the parent here gives
                // matchers based on this config an avenue to match the rate source's parent.
                //
                final Rates rates = context.getRatesWithParentRatesAsFallback();
                final RateLimiter rateLimiter = rateLimiterProvider.getRateLimiter(mainMatch, rates);

                visitor.accept(mainMatch, rateLimiter);

                if (LOG.isTraceEnabled()) {
                    LOG.trace("{} = {}", mainMatch, rateLimiter);
                }
                return 1;
            } else {
                return 0;
            }
        }
    }

    private static class PermitAcquiringVisitor implements BiConsumer<String, RateLimiter> {
        private final int permits;
        private double totalTime = 0;
        private PermitAcquiringVisitor(int permits) {
            this.permits = permits;
        }
        @Override
        public void accept(String match, RateLimiter rateLimiter) {
            double timeSpent = rateLimiter.acquire(permits);
            if (timeSpent > 0) { // Only increment when > 0, as some value may be negative.
                totalTime += timeSpent;
            }
        }
    }

    private static class PermitAttemptingVisitor implements BiConsumer<String, RateLimiter> {
        private final int permits;
        private final long timeout;
        private final TimeUnit timeUnit;
        private boolean noLimitExceeded = true;
        private PermitAttemptingVisitor(int permits, long timeout, TimeUnit timeUnit) {
            this.permits = permits;
            this.timeout = timeout;
            this.timeUnit = Objects.requireNonNull(timeUnit);
        }
        @Override
        public void accept(String match, RateLimiter rateLimiter) {
            if (!rateLimiter.tryAcquire(permits, timeout, timeUnit)) {
                noLimitExceeded = false;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RateLimiterComposite@").append(Integer.toHexString(hashCode())).append("{");
        rootNode.visitAll(node -> {
            acceptMatchingRateLimiters(node,
                    (match, rateLimiter) -> {
                        if (builder.length() > 0) {
                            builder.append(", ");
                        }
                        builder.append(match).append("=").append(rateLimiter);
                    });
        });
        builder.append("}");
        return builder.toString();
    }
}
