package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.Ticker;

import java.util.*;

final class DefaultRateLimiterProvider implements RateLimiterProvider {

    private final BandwidthStoreFacade bandwidthStoreFacade;
    private final Ticker ticker;

    private final Map<Object, RateLimiter> keyToRateLimiterMap;

    DefaultRateLimiterProvider(
            RateToBandwidthConverter rateToBandwidthConverter,
            BandwidthsStore<?> bandwidthStore,
            Ticker ticker) {
        this.bandwidthStoreFacade =
                new BandwidthStoreFacade<>(rateToBandwidthConverter, bandwidthStore);
        this.ticker = Objects.requireNonNull(ticker);
        this.keyToRateLimiterMap = new WeakHashMap<>();
    }

    @Override
    public RateLimiter getRateLimiter(String key, Rate rate) {
        RateLimiter rateLimiter;
        if ((rateLimiter = this.keyToRateLimiterMap.get(key)) == null) {
            rateLimiter = createRateLimiter(key, rate);
            this.keyToRateLimiterMap.put(key, rateLimiter);
        }
        return rateLimiter;
    }

    @Override
    public RateLimiter getRateLimiter(String key, Rates rates) {
        RateLimiter rateLimiter;
        if ((rateLimiter = this.keyToRateLimiterMap.get(key)) == null) {
            rateLimiter = createRateLimiter(key, rates);
            this.keyToRateLimiterMap.put(key, rateLimiter);
        }
        return rateLimiter;
    }

    private RateLimiter createRateLimiter(String key, Rate rate) {
        Bandwidth bandwidth = bandwidthStoreFacade.getOrCreateBandwidth(key, rate);
        return RateLimiter.of(bandwidth, ticker);
    }

    private RateLimiter createRateLimiter(String key, Rates rates) {
        Bandwidth bandwidth = bandwidthStoreFacade.getOrCreateBandwidth(key, rates);
        return RateLimiter.of(bandwidth, ticker);
    }
}
