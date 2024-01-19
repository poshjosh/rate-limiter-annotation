package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotations.Experimental;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Operator;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Experimental
final class RateLimiterCompositeBottomUp<K> extends RateLimiterComposite<K> {
    private final Node<LimiterContext<K>>[] leafNodes;

    RateLimiterCompositeBottomUp(K key,
            Node<LimiterContext<K>> rootNode,
            RateLimiterProvider rateLimiterProvider) {
        super(key, rootNode, rateLimiterProvider);
        this.leafNodes = collectLeafs(rootNode);
    }
    private static <R> Node<LimiterContext<R>> [] collectLeafs(Node<LimiterContext<R>> node) {
        Set<Node<LimiterContext<R>>> leafNodes = new LinkedHashSet<>();
        Predicate<Node<LimiterContext<R>>> test = n -> n.isLeaf() && n.hasValue();
        node.getRoot().visitAll(test, leafNodes::add);
        return leafNodes.toArray(new Node[0]);
    }

    @Override
    public double acquire(int permits) {
        Set<String> attempts = new HashSet<>();
        AtomicLong totalTime = new AtomicLong();
        for (Node<LimiterContext<K>> node : leafNodes) {
            do {
                acceptMatchingRateLimiters(node, (match, rateLimiter) -> {
                    if (!attempts.add(match)) {
                        return;
                    }
                    double timeSpent = rateLimiter.acquire(permits);
                    if (timeSpent > 0) { // Only increment when > 0, as some value may be negative.
                        totalTime.addAndGet((long)timeSpent);
                    }
                });
                node = node.getParentOrDefault(null);
            } while(node != null);
        }
        return totalTime.get();
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        Set<String> attempts = new HashSet<>();
        AtomicInteger successes = new AtomicInteger();
        outer:
        for (Node<LimiterContext<K>> node : leafNodes) {
            do {
                final int matchCount = acceptMatchingRateLimiters(node, (match, rateLimiter) -> {
                    if (attempts.add(match) && rateLimiter.tryAcquire(permits, timeout, unit)) {
                        successes.incrementAndGet();
                    }
                });
                // For bottom-up traversal, we can stop at the first match.
                if (matchSucceeded(node, matchCount)) {
                    break outer;
                }
                node = node.getParentOrDefault(null);
            } while(node != null);
        }
        return attempts.size() == successes.get();
    }

    private boolean matchSucceeded(Node<LimiterContext<K>> node, int matchCount) {
        LimiterContext<K> context = node.getValueOrDefault(null);
        if (context == null) {
            return false;
        }
        if (context.hasSubConditions()) {
            return matchCount >= context.getSubMatchers().size();
        } else {
            return matchCount >= 1;
        }
    }

    // We use Sets to prevent duplicates.
    // Since we visit from leaf nodes upwards to root, we often encounter
    // the same parent rate limiter multiple times. This happens because
    // 2 or more leaf nodes may share the same parent.
    //

    @Override
    public Bandwidth getBandwidth() {
        Set<String> attempts = new HashSet<>();
        List<Bandwidth> bandwidths = new ArrayList<>();
        for (Node<LimiterContext<K>> node : leafNodes) {
            do {
                acceptMatchingRateLimiters(node, (match, rateLimiter) -> {
                    if (!attempts.add(match)) {
                        return;
                    }
                    bandwidths.add(rateLimiter.getBandwidth());
                });
                node = node.getParentOrDefault(null);
            } while(node != null);
        }
        if (bandwidths.isEmpty()) {
            throw new IllegalStateException("No bandwidths specified for this RateLimiter.");
        }
        if (bandwidths.size() == 1) {
            return bandwidths.get(0);
        }
        // For multiple Bandwidths conjugated with Operator.OR, the composed Bandwidth
        // succeeds only when all Bandwidths succeed. This is the case here.
        return Bandwidths.of(Operator.OR, bandwidths.toArray(new Bandwidth[0]));
    }
}
