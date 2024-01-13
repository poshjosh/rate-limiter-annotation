package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.Ticker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class DefaultRateLimiterProvider<K> implements RateLimiterProvider<K> {

    private final BandwidthStoreFacade<K> bandwidthStoreFacade;
    private final Ticker ticker;

    private final Map<Object, RateLimiter> resourceIdToRateLimiter;

    DefaultRateLimiterProvider(
            RateToBandwidthConverter rateToBandwidthConverter,
            BandwidthsStore<K> bandwidthStore,
            Ticker ticker) {
        this.bandwidthStoreFacade =
                new BandwidthStoreFacade<>(rateToBandwidthConverter, bandwidthStore);
        this.ticker = Objects.requireNonNull(ticker);
        this.resourceIdToRateLimiter = new ConcurrentHashMap<>();
    }

    @Override
    public RateLimiter getRateLimiter(K key, Rate rate) {
        RateLimiter rateLimiter;
        if ((rateLimiter = this.resourceIdToRateLimiter.get(key)) == null) {
            rateLimiter = createRateLimiter(key, rate);
            this.resourceIdToRateLimiter.put(key, rateLimiter);
        }
        return rateLimiter;
    }

    @Override
    public RateLimiter getRateLimiter(K key, Rates rates) {
        RateLimiter rateLimiter;
        if ((rateLimiter = this.resourceIdToRateLimiter.get(key)) == null) {
            rateLimiter = createRateLimiter(key, rates);
            this.resourceIdToRateLimiter.put(key, rateLimiter);
        }
        return rateLimiter;
    }

    private RateLimiter createRateLimiter(K key, Rate rate) {
        Bandwidth bandwidth = bandwidthStoreFacade.getOrCreateBandwidth(key, rate);
        return RateLimiter.of(bandwidth, ticker);
    }

    private RateLimiter createRateLimiter(K key, Rates rates) {
        Bandwidth bandwidth = bandwidthStoreFacade.getOrCreateBandwidth(key, rates);
        return RateLimiter.of(bandwidth, ticker);
    }
}
