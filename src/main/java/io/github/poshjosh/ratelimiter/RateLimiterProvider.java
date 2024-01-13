package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.Ticker;

public interface RateLimiterProvider<K> {

    static <K> RateLimiterProvider<K> ofDefaults() {
        final Ticker ticker = Ticker.ofDefaults();
        return of(RateToBandwidthConverter.ofDefaults(ticker), BandwidthsStore.ofDefaults(), ticker);
    }

    static <K> RateLimiterProvider<K> of(
            RateToBandwidthConverter converter,
            BandwidthsStore<K> store,
            Ticker ticker) {
        return new DefaultRateLimiterProvider<>(converter, store, ticker);
    }

    RateLimiter getRateLimiter(K key, Rate rate);

    RateLimiter getRateLimiter(K key, Rates rates);
}
