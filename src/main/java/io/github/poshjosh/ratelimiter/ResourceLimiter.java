package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MICROSECONDS;

/**
 * Uses multiple {@link RateLimiter}s to restrict consumption of multiple resources identified by IDs.
 *
 * @param <K> The type of the ID for each resource
 * @see io.github.poshjosh.ratelimiter.RateLimiter
 */
public interface ResourceLimiter<K> {

    ResourceLimiter<Object> NO_OP = new ResourceLimiter<Object>() {
        @Override public ResourceLimiter<Object> listener(UsageListener listener) { return this; }
        @Override public UsageListener getListener() { return UsageListener.NO_OP; }
        @Override public boolean tryConsume(Object key, int permits, long timeout, TimeUnit unit) {
            return true;
        }
        @Override public String toString() {
            return "ResourceLimiter$NO_OP";
        }
    };

    @SuppressWarnings("unchecked")
    static <K> ResourceLimiter<K> noop() {
        return (ResourceLimiter<K>) NO_OP;
    }

    /**
     * Create a ResourceLimiter for the specified classes
     * @param sourceOfRateLimitInfo The classes which contain rate limit related annotations.
     * @return A ResourceLimiter instance.
     * @param <K> The type of the ID for each resource
     */
    static <K> ResourceLimiter<K> of(Class<?>... sourceOfRateLimitInfo) {
        return of(RateProcessor.ofDefaults().processAll(sourceOfRateLimitInfo));
    }

    static <K> ResourceLimiter<K> of(String resourceId, Rate limit) {
        return of(resourceId, RateConfig.of(RateSource.of(resourceId, limit != null), Rates.of(limit)));
    }

    static <K> ResourceLimiter<K> of(String resourceId, Operator operator, Rate... limits) {
        final boolean hasLimits = limits != null && limits.length > 0;
        return of(resourceId, RateConfig.of(RateSource.of(resourceId, hasLimits),
                Rates.of(operator, limits)));
    }

    static <K> ResourceLimiter<K> of(String resourceId, RateConfig rateConfig) {
        return of(Node.of(resourceId, rateConfig, Node.ofDefaultRoot()));
    }

    static <K> ResourceLimiter<K> of(Node<RateConfig> node) {
        return of(UsageListener.NO_OP, RateLimiterProvider.ofDefaults(),
                MatcherProvider.ofDefaults(), node);
    }

    static <K> ResourceLimiter<K> of(
            UsageListener listener,
            RateLimiterProvider<String> rateLimiterProvider,
            MatcherProvider<K> matcherProvider,
            Node<RateConfig> node) {
        Function<Node<RateConfig>, LimiterContext<K>> transformer = currentNode -> {
            return LimiterContext.of(matcherProvider, currentNode);
        };
        Node<LimiterContext<K>> limiterNode = node.getRoot().transform(transformer);
        return of(listener, rateLimiterProvider, limiterNode);
    }

    static <K> ResourceLimiter<K> of(
            UsageListener listener,
            RateLimiterProvider<String> rateLimiterProvider,
            Node<LimiterContext<K>> node) {
        return new DefaultResourceLimiter<>(
                listener, new RateLimiterTree<>(rateLimiterProvider, node));
    }

    ResourceLimiter<K> listener(UsageListener listener);

    UsageListener getListener();

    /**
     * Consumes the given number of permits from this {@code ResourceLimiter} if it can be obtained
     * without exceeding the specified {@code timeout}, or returns {@code false} immediately (without
     * waiting) if the permits would not have been granted before the timeout expired.
     *
     * @param key the resource to acquire permits for
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits. Negative values are treated as zero.
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the permits were acquired, {@code false} otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     */
    boolean tryConsume(K key, int permits, long timeout, TimeUnit unit);

    /**
     * Acquires a permit from this {@link ResourceLimiter} if it can be acquired immediately without
     * delay.
     *
     * <p>This method is equivalent to {@code tryConsume(1)}.
     *
     * @param key the key to acquire permits for
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     */
    default boolean tryConsume(K key) {
        return tryConsume(key, 1, 0, MICROSECONDS);
    }

    /**
     * Acquires a permit from this {@code ResourceLimiter} if it can be obtained without exceeding
     * the specified {@code timeout}, or returns {@code false} immediately (without waiting) if
     * the permit would not have been granted before the timeout expired.
     *
     * <p>This method is equivalent to {@code tryConsume(1, timeout)}.
     *
     * @param key the key to acquire permits for
     * @param timeout the maximum time to wait for the permit. Negative values are treated as zero.
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     */
    default boolean tryConsume(K key, Duration timeout) {
        return tryConsume(key, 1, Util.toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
    }

    /**
     * Acquires a permit from this {@code ResourceLimiter} if it can be obtained without exceeding the
     * specified {@code timeout}, or returns {@code false} immediately (without waiting) if the permit
     * would not have been granted before the timeout expired.
     *
     * <p>This method is equivalent to {@code tryConsume(1, timeout, unit)}.
     *
     * @param key the key to acquire permits for
     * @param timeout the maximum time to wait for the permit. Negative values are treated as zero.
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     */
    default boolean tryConsume(K key, long timeout, TimeUnit unit) {
        return tryConsume(key, 1, timeout, unit);
    }

    /**
     * Acquires permits from this {@link ResourceLimiter} if it can be acquired immediately without delay.
     *
     * <p>This method is equivalent to {@code tryConsume(permits, 0, anyUnit)}.
     *
     * @param key the key to acquire permits for
     * @param permits the number of permits to acquire
     * @return {@code true} if the permits were acquired, {@code false} otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     */
    default boolean tryConsume(K key, int permits) {
        return tryConsume(key, permits, 0, MICROSECONDS);
    }

    /**
     * Acquires the given number of permits from this {@code ResourceLimiter} if it can be obtained
     * without exceeding the specified {@code timeout}, or returns {@code false} immediately (without
     * waiting) if the permits would not have been granted before the timeout expired.
     *
     * @param key the key to acquire permits for
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits. Negative values are treated as zero.
     * @return {@code true} if the permits were acquired, {@code false} otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     */
    default boolean tryConsume(K key, int permits, Duration timeout) {
        return tryConsume(key, permits, Util.toNanosSaturated(timeout), TimeUnit.NANOSECONDS);
    }

    /**
     * Returns a composed ResourceLimiter that first calls this ResourceLimiter's tryConsume function,
     * AND (&&) then calls the tryConsume function of the {@code after} ResourceLimiter.
     * If evaluation of either tryConsume function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param after The ResourceLimiter to tryConsume after this ResourceLimiter
     * @return a composed ResourceLimiter that firsts calls this AND (&&) then the {@code after} ResourceLimiter
     * @throws NullPointerException if after is null
     */
    default ResourceLimiter<K> andThen(ResourceLimiter<K> after) {
        Objects.requireNonNull(after);
        final UsageListener listener = getListener().andThen(after.getListener());
        return new ResourceLimiter<K>() {
            @Override public ResourceLimiter<K> listener(UsageListener listener) {
                return ResourceLimiter.this.listener(listener).andThen(after.listener(listener));
            }
            @Override public UsageListener getListener() {
                return listener;
            }
            @Override public boolean tryConsume(K key, int permits, long timeout, TimeUnit unit) {
                return ResourceLimiter.this.tryConsume(key, permits, timeout, unit)
                        && after.tryConsume(key, permits, timeout, unit);
            }
        };
    }
}
