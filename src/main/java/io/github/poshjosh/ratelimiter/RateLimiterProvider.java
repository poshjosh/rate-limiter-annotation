package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.Ticker;

public interface RateLimiterProvider {

    static RateLimiterProvider ofDefaults() {
        final Ticker ticker = Ticker.ofDefaults();
        return of(RateToBandwidthConverter.ofDefaults(ticker), BandwidthsStore.ofDefaults(), ticker);
    }

    static RateLimiterProvider of(
            RateToBandwidthConverter converter,
            BandwidthsStore<?> store,
            Ticker ticker) {
        return new DefaultRateLimiterProvider(converter, store, ticker);
    }

    RateLimiter getRateLimiter(String key, Rate rate);

    RateLimiter getRateLimiter(String key, Rates rates);
}
