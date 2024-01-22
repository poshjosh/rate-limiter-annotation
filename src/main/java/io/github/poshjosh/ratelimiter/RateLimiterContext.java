package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;
import io.github.poshjosh.ratelimiter.util.Ticker;

import java.util.Map;
import java.util.Set;

public interface RateLimiterContext<K> {

    /**
     * Users are required (at the minimum) to provide: {@link Builder#properties(RateLimitProperties)}
     * @return A builder for {@link RateLimiterContext}
     */
    static <K> Builder<K> builder() {
        return new RateLimiterContextBuilder<>();
    }

    /**
     * Users are required (at the minimum) to provide: {@link Builder#properties(RateLimitProperties)}
     */
    interface Builder<K> {

        RateLimiterContext<K> build();

        /**
         * Specify either this or {@code #properties(RateLimitProperties)}
         * @param packages
         * @return
         */
        Builder<K> packages(String... packages);

        /**
         * Specify either this or {@code #properties(RateLimitProperties)}
         * @param classes The classes containing rate limit related annotations
         * @return
         */
        Builder<K> classes(Class<?>... classes);

        /**
         * Specify either this or {@code #properties(RateLimitProperties)}
         * @param rates
         * @return
         */
        Builder<K> rates(Map<String, Rates> rates);

        /**
         * Specify either this or {@code #classes(Class...)}
         * @param properties The properties containing rate limit specifications
         * @return this builder
         */
        Builder<K> properties(RateLimitProperties properties);

        /**
         * <p><b>Not mandatory.</b> If not specified an default will be created</p>
         * @param matcherProvider For providing matchers which determine if rate
         *                        limiting should be applied.
         * @return this builder
         */
        Builder<K> matcherProvider(MatcherProvider<K> matcherProvider);

        /**
         * <p><b>Not mandatory</b></p>
         * @param rateLimiterProvider For provider rate limiters
         * @return this builder
         */
        Builder<K> rateLimiterProvider(RateLimiterProvider rateLimiterProvider);

        /**
         * <p><b>Not mandatory.</b> If not specified an in-memory instance is used</p>
         * @param store For storing bandwidths
         * @return this builder
         */
        Builder<K> store(BandwidthsStore<?> store);

        /**
         * <p><b>Not mandatory</b></p>
         * @param ticker The ticker to keep track of time.
         * @return this builder
         */
        Builder<K> ticker(Ticker ticker);
    }

    default boolean isRateLimited() {
        return isRateLimitingEnabled() && hasRateSources();
    }

    default boolean isRateLimitingEnabled() {
        return getProperties().isRateLimitingEnabled();
    }

    boolean hasRateSources();

    /**
     * Return a list of classes specified in {@code RateLimitProperties}, either directly
     * or via package names.
     * @return The classes which are targets for rate limiting
     */
    Set<Class<?>> getTargetClasses();

    RateLimitProperties getProperties();

    RateLimiterContext<K> withProperties(RateLimitProperties properties);

    MatcherProvider<K> getMatcherProvider();

    RateLimiterContext<K> withMatcherProvider(MatcherProvider<K> matcherProvider);

    RateLimiterProvider getRateLimiterProvider();

    RateLimiterContext<K> withRateLimiterProvider(RateLimiterProvider rateLimiterProvider);

    BandwidthsStore<?> getStore();

    RateLimiterContext<K> withStore(BandwidthsStore<K> bandwidthsStore);

    Ticker getTicker();

    RateLimiterContext<K> withTicker(Ticker ticker);
}
