package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.RateConfig;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class DefaultBandwidthsContext<K> implements BandwidthsContext<K>{

    private final RateToBandwidthConverter rateToBandwidthConverter =
            RateToBandwidthConverter.ofDefaults();

    private final SleepingTicker sleepingTicker;

    private final BandwidthsStore<K> store;

    private final ReadWriteLock storeLock = new ReentrantReadWriteLock();

    private final Map<Object, RateLimiter> resourceIdToRateLimiters;

    DefaultBandwidthsContext(SleepingTicker sleepingTicker, BandwidthsStore<K> store) {
        this.sleepingTicker = Objects.requireNonNull(sleepingTicker);
        this.store = Objects.requireNonNull(store);
        this.resourceIdToRateLimiters = new ConcurrentHashMap<>();
    }

    @Override public SleepingTicker getTicker(K key) {
        return sleepingTicker;
    }

    @Override
    public Bandwidths getBandwidths(K key, RateConfig rateConfig) {
        final Bandwidths existing = getBandwidthsFromStore(key);
        return existing == null ? createBandwidths(key, rateConfig) : existing;
    }

    private Bandwidths createBandwidths(K key, RateConfig rateConfig) {
        return createBandwidths(rateConfig).with(getTicker(key).elapsedMicros());
    }

    private Bandwidths createBandwidths(RateConfig rateConfig) {
        return rateToBandwidthConverter.convert(rateConfig.getValue());
    }

    private Bandwidths getBandwidthsFromStore(K key) {
        try{
            storeLock.readLock().lock();
            return store.get(key);
        }finally {
            storeLock.readLock().unlock();
        }
    }

    @Override
    public void onChange(K key, Bandwidths bandwidths) {
        try {
            storeLock.writeLock().lock();
            store.put(key, bandwidths);
        }finally {
            storeLock.writeLock().unlock();
        }
    }

    public RateLimiter getLimiter(K key, Bandwidths bandwidths) {
        RateLimiter value;
        if ((value = this.resourceIdToRateLimiters.get(key)) == null) {
            RateLimiter newValue;
            if ((newValue = createNewLimiter(key, bandwidths)) != null) {
                this.resourceIdToRateLimiters.put(key, newValue);
                return newValue;
            }
        }
        return value;
    }

    private RateLimiter createNewLimiter(K key, Bandwidths bandwidths) {
        return RateLimiter.of(bandwidths, getTicker(key));
    }
}
