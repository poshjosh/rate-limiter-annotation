package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.Ticker;
import io.github.poshjosh.ratelimiter.util.Tickers;

public interface RateLimiterProviders {
    static RateLimiterProvider ofDefaults() {
        final Ticker ticker = Tickers.ofDefaults();
        return of(RateToBandwidthConverter.of(ticker), BandwidthsStore.ofDefaults(), ticker);
    }

    static RateLimiterProvider of(RateToBandwidthConverter converter, BandwidthsStore<?> store,
            Ticker ticker) {
        return new DefaultRateLimiterProvider(converter, store, ticker);
    }
}
