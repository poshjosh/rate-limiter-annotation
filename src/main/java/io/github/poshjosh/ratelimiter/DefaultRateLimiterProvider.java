package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.LimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class DefaultRateLimiterProvider<R, K> implements RateLimiterProvider<R, K> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRateLimiterProvider.class);

    private final BandwidthsStore<K> store;

    private final ReadWriteLock storeLock = new ReentrantReadWriteLock();

    private final Map<Object, RateLimiter> resourceIdToRateLimiter;

    DefaultRateLimiterProvider(BandwidthsStore<K> store) {
        this.store = Objects.requireNonNull(store);
        this.resourceIdToRateLimiter = new ConcurrentHashMap<>();
    }

    public RateLimiter getRateLimiter(K key, LimiterConfig<R> limiterConfig, int index) {
        RateLimiter value;
        if ((value = this.resourceIdToRateLimiter.get(key)) == null) {
            value = createLimiter(key, limiterConfig, index);
            this.resourceIdToRateLimiter.put(key, value);
        }
        return value;
    }

    private RateLimiter createLimiter(K key, LimiterConfig<R> config, int index) {
        Bandwidth bandwidth = getOrCreateBandwidth(key, config, index);
        bandwidth = withAutoSave(key, bandwidth);
        return RateLimiter.of(bandwidth, config.getTicker());
    }

    private Bandwidth getOrCreateBandwidth(K key, LimiterConfig<R> config, int index) {
        final Bandwidth existing = getBandwidthFromStore(key);
        return existing == null ? createBandwidth(config, index) : existing;
    }

    private Bandwidth createBandwidth(LimiterConfig<R> config, int index) {
        Bandwidth [] bandwidths = config.getBandwidths();
        if (bandwidths.length == 0) {
            if (index == 0) {
                return Bandwidth.UNLIMITED;
            }
            throw noLimitAtIndex(config, index);
        }
        return bandwidths[index].with(config.getTicker().elapsedMicros());
    }

    private IndexOutOfBoundsException noLimitAtIndex(LimiterConfig<R> config, int index) {
        return new IndexOutOfBoundsException("Index: " + index +
                ", exceeds number of rate limits defined at: " + config.getSource().getSource());
    }

    private Bandwidth withAutoSave(K key, Bandwidth bandwidth) {
        return new BandwidthWrapper(bandwidth) {
            @Override public long reserveEarliestAvailable(int permits, long nowMicros) {
                final long result = super.reserveEarliestAvailable(permits, nowMicros);
                DefaultRateLimiterProvider.this.saveBandwidthToStore(key, bandwidth);
                return result;
            }
            @Override public String toString() {
                return bandwidth.toString();
            }
        };
    }

    private Bandwidth getBandwidthFromStore(K key) {
        try{
            storeLock.readLock().lock();
            return store.get(key);
        }finally {
            storeLock.readLock().unlock();
        }
    }

    private void saveBandwidthToStore(K key, Bandwidth bandwidth) {
        try {
            storeLock.writeLock().lock();
            store.put(key, bandwidth);
            LOG.trace("Saved: {} = {}", key, bandwidth);
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
