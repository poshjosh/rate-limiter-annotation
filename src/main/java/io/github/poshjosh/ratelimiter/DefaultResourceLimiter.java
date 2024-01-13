package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.util.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Uses multiple {@link RateLimiter}s to restrict consumption of multiple resources identified by IDs.
 *
 * A {@link io.github.poshjosh.ratelimiter.node.Node} is used to hold the data for either:
 *
 * <ul>
 *     <li>An annotation - In which case it is a group node</li>
 *     <li>A class</li>
 *     <li>A method</li>
 * </ul>
 *
 * Each {@link io.github.poshjosh.ratelimiter.node.Node} may have:
 *
 * <ul>
 *     <li>Zero or more rates defined</li>
 *     <li>A group annotation linking the node to a group</li>
 *     <li>Zero or more rates defined at the declaration of the annotation for the group</li>
 * </ul>
 *
 * Each node is identified in the store by a key, usually the match result returned by the node's
 * main {@link io.github.poshjosh.ratelimiter.util.Matcher}.
 *
 * <ul>
 *     <li>Group - the match result, otherwise the node name if match result is null</li>
 *     <li>Class - the match result</li>
 *     <li>Method - the match result</li>
 * </ul>
 *
 * In addition to each node's main {@link io.github.poshjosh.ratelimiter.util.Matcher}, ancillary
 * matchers may be defined. Ancillary matching takes effect when there is no composition.
 * Composition is the presence of multiple {@link io.github.poshjosh.ratelimiter.annotations.Rate}
 * annotations composed by an {@link Operator} (e.g OR, AND, etc).
 *
 * A single {@link io.github.poshjosh.ratelimiter.node.Node} is passed to this class. From that node,
 * all the leaf nodes are collected. Every request is used to visit each leaf node. For each
 * leaf node, its parent node is visited recursively till the root node (which is not visited).
 *
 * @param <K> The type of the resource which is consumed
 */
final class DefaultResourceLimiter<K> implements ResourceLimiter<K> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultResourceLimiter.class);

    private final UsageListener listener;

    private final RateLimiterTree<K> rateLimiterTree;

    DefaultResourceLimiter(
            UsageListener listener,
            RateLimiterTree<K> rateLimiterTree) {
        this.listener = Objects.requireNonNull(listener);
        this.rateLimiterTree = Objects.requireNonNull(rateLimiterTree);
    }

    @Override
    public DefaultResourceLimiter<K> listener(UsageListener listener) {
        return new DefaultResourceLimiter<>(listener, rateLimiterTree);
    }

    @Override
    public UsageListener getListener() {
        return listener;
    }

    @Override
    public boolean tryConsume(K key, int permits, long timeout, TimeUnit unit) {

        final AtomicBoolean result = new AtomicBoolean(true);

        final RateLimiterTree.RateLimiterConsumer<K> visitor =
                (match, rateLimiter, context, index, total) -> {

                    final boolean success = rateLimiter.tryAcquire(permits, timeout, unit);
                    LOG.trace("SUCCESS: {}, key = {}, limiter = {}", success, key, rateLimiter);
                    if (success) {
                        listener.onConsumed(key, match, permits, context.getRateConfig());
                    } else {
                        listener.onRejected(key, match, permits, context.getRateConfig());
                        result.set(false);
                    }
                };

        rateLimiterTree.visitRateLimiters(key, visitor);

        return result.get();
    }

    @Override
    public String toString() {
        return "DefaultResourceLimiter{" + "listener=" + listener + ", rateLimiterTree="
                + rateLimiterTree + '}';
    }
}
