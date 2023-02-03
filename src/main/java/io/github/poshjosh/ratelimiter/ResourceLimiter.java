package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MICROSECONDS;

public interface ResourceLimiter<K> {

    ResourceLimiter<Object> NO_OP = new ResourceLimiter<Object>() {
        @Override public ResourceLimiter<Object> listener(UsageListener listener) { return this; }
        @Override public UsageListener getListener() { return UsageListener.NO_OP; }
        @Override public boolean tryConsume(Object key, int permits, long timeout, TimeUnit unit) {
            return true;
        }
    };

    @SuppressWarnings("unchecked")
    static <R> ResourceLimiter<R> noop() {
        return (ResourceLimiter<R>) NO_OP;
    }

    static <R> ResourceLimiter<R> of(Class<?>... sourceOfRateLimitInfo) {
        return of(RateProcessor.ofDefaults().processAll(sourceOfRateLimitInfo));
    }

    static <R> ResourceLimiter<R> of(String resourceId, Rate... limits) {
        return of(resourceId, RateConfig.of(Rates.of(limits)));
    }

    static <R> ResourceLimiter<R> of(String resourceId, RateConfig rateConfig) {
        return of(Node.of(resourceId, rateConfig, Node.of("root")));
    }

    static <R> ResourceLimiter<R> of(Node<RateConfig> node) {
        return of(
                UsageListener.NO_OP, SleepingTicker.zeroOffset(),
                BandwidthsStore.ofDefaults(), (MatcherProvider)MatcherProvider.ofDefaults(),
                node
        );
    }

    static <R> ResourceLimiter<R> of(
            UsageListener listener,
            BandwidthsStore<Object> store,
            MatcherProvider<R, Object> matcherProvider,
            Node<RateConfig> rootNode) {
        return of(listener, SleepingTicker.zeroOffset(), store, matcherProvider, rootNode);
    }

    static <R> ResourceLimiter<R> of(
            UsageListener listener,
            SleepingTicker ticker,
            BandwidthsStore<Object> store,
            MatcherProvider<R, Object> matcherProvider,
            Node<RateConfig> node) {
        RateToBandwidthConverter converter = RateToBandwidthConverter.ofDefaults();
        Function<Node<RateConfig>, LimiterConfig<R, Object>> transformer = n -> {
             RateConfig rateConfig = n.getValueOrDefault(null);
             if (rateConfig == null) {
                 return null;
             }
             return LimiterConfig.of(n, converter, matcherProvider, ticker);
        };
        return new DefaultResourceLimiter<>(listener, LimiterProvider.of(store),
                node.getRoot().transform(transformer));
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
     * <p>This method is equivalent to {@code tryAcquire(1)}.
     *
     * @param key the key to acquire permits for
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     */
    default boolean tryConsume(K key) {
        return tryConsume(key, 1, 0, MICROSECONDS);
    }

    /**
     * Acquires a permit from this {@code ResourceLimiter} if it can be obtained without exceeding the
     * specified {@code timeout}, or returns {@code false} immediately (without waiting) if the permit
     * would not have been granted before the timeout expired.
     *
     * <p>This method is equivalent to {@code tryAcquire(1, timeout)}.
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
     * <p>This method is equivalent to {@code tryAcquire(1, timeout, unit)}.
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
     * <p>This method is equivalent to {@code tryAcquire(permits, 0, anyUnit)}.
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
