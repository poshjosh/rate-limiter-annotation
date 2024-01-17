package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.*;
import java.util.*;

/**
 * A builder for {@link RateLimiterContext}.
 * This build provides sensible defaults.
 * @param <K> the type of the key accepted by Matchers
 */
final class RateLimiterContextBuilder<K> implements RateLimiterContext.Builder<K> {

    private final RateLimiterContextImpl<K> context;

    RateLimiterContextBuilder() {
        this.context = new RateLimiterContextImpl<>();
    }

    @Override
    public RateLimiterContext<K> build() {
        return context.withDefaultsAsFallback();
    }

    @Override
    public RateLimiterContext.Builder<K> packages(String... packages) {
        context.setPackages(packages);
        return this;
    }

    @Override
    public RateLimiterContext.Builder<K> classes(Class<?>... classes) {
        context.setClasses(classes);
        return this;
    }

    @Override
    public RateLimiterContext.Builder<K> rates(Map<String, Rates> rates) {
        context.setRates(rates);
        return this;
    }

    @Override
    public RateLimiterContext.Builder<K> properties(RateLimitProperties properties) {
        context.setProperties(properties);
        return this;
    }

    @Override
    public RateLimiterContext.Builder<K> matcherProvider(MatcherProvider<K> matcherProvider) {
        context.setMatcherProvider(matcherProvider);
        return this;
    }

    @Override
    public RateLimiterContext.Builder<K> rateLimiterProvider(
            RateLimiterProvider rateLimiterProvider) {
        context.setRateLimiterProvider(rateLimiterProvider);
        return this;
    }

    @Override
    public RateLimiterContext.Builder<K> store(BandwidthsStore<?> store) {
        context.setStore(store);
        return this;
    }

    @Override
    public RateLimiterContext.Builder<K> ticker(Ticker ticker) {
        context.setTicker(ticker);
        return this;
    }
}
