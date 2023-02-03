package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.LimiterConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class DefaultLimiterProvider<R, K> implements LimiterProvider<R, K> {

    private final BandwidthsStore<K> store;

    private final ReadWriteLock storeLock = new ReentrantReadWriteLock();

    private final Map<Object, List<RateLimiter>> resourceIdToRateLimiters;

    DefaultLimiterProvider(BandwidthsStore<K> store) {
        this.store = Objects.requireNonNull(store);
        this.resourceIdToRateLimiters = new ConcurrentHashMap<>();
    }

    @Override
    public List<RateLimiter> getLimiters(K key, String name, LimiterConfig<R, K> limiterConfig) {
        List<RateLimiter> value;
        if ((value = this.resourceIdToRateLimiters.get(key)) == null) {
            value = createLimiters(key, limiterConfig);
            this.resourceIdToRateLimiters.put(key, value);
        }
        return value;
    }

    private List<RateLimiter> createLimiters(K key, LimiterConfig<R, K> config) {
        Bandwidth [] bandwidths = getOrCreateBandwidths(key, config);
        RateLimiter [] limiters = new RateLimiter[bandwidths.length];
        for(int i = 0; i < bandwidths.length; i++) {
            limiters[i] = RateLimiter.of(bandwidths[i], config.getSleepingTicker());
        }
        return Arrays.asList(limiters);
    }

    private Bandwidth [] getOrCreateBandwidths(K key, LimiterConfig<R, K> config) {
        final Bandwidth [] existing = getBandwidthFromStore(key);
        return existing == null ? createBandwidths(key, config) : existing;
    }

    private Bandwidth[] createBandwidths(K key, LimiterConfig<R, K> config) {
        SleepingTicker ticker = config.getSleepingTicker();
        Bandwidth [] bandwidths = config.getBandwidths();
        Bandwidth [] result = new Bandwidth[bandwidths.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = withAutoSave(key, bandwidths[i].with(ticker.elapsedMicros()), result);
        }
        return result;
    }

    private Bandwidth withAutoSave(K key, Bandwidth bandwidth, Bandwidth[] group) {
        return new BandwidthWrapper(bandwidth) {
            @Override public long reserveEarliestAvailable(int permits, long nowMicros) {
                final long result = super.reserveEarliestAvailable(permits, nowMicros);
                DefaultLimiterProvider.this.saveBandwidthToStore(key, group);
                return result;
            }
        };
    }

    private Bandwidth[] getBandwidthFromStore(K key) {
        try{
            storeLock.readLock().lock();
            return store.get(key);
        }finally {
            storeLock.readLock().unlock();
        }
    }

    private void saveBandwidthToStore(K key, Bandwidth [] bandwidths) {
        try {
            storeLock.writeLock().lock();
            store.put(key, bandwidths);
        }finally {
            storeLock.writeLock().unlock();
        }
    }

    private static class BandwidthWrapper implements Bandwidth{
        private final Bandwidth delegate;
        private BandwidthWrapper(Bandwidth delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }
        @Override
        public Bandwidth with(long nowMicros) {
            return delegate.with(nowMicros);
        }
        @Override
        public long queryEarliestAvailable(long nowMicros) {
            return delegate.queryEarliestAvailable(nowMicros);
        }
        @Override
        public long reserveEarliestAvailable(int permits, long nowMicros) {
            return delegate.reserveEarliestAvailable(permits, nowMicros);
        }
        @Override
        public double getPermitsPerSecond() {
            return delegate.getPermitsPerSecond();
        }
        @Override public boolean equals(Object o) { return delegate.equals(o); }
        @Override public int hashCode() { return delegate.hashCode(); }
        @Override public String toString() { return delegate.toString(); }
    }
}
