package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class BandwidthStoreFacade<K> {

    private static final Logger LOG = LoggerFactory.getLogger(BandwidthStoreFacade.class);

    private final RateToBandwidthConverter rateToBandwidthConverter;
    private final BandwidthsStore<K> store;

    private final ReadWriteLock storeLock = new ReentrantReadWriteLock();

    BandwidthStoreFacade(
            RateToBandwidthConverter rateToBandwidthConverter,
            BandwidthsStore<K> bandwidthsStore) {
        this.rateToBandwidthConverter = Objects.requireNonNull(rateToBandwidthConverter);
        this.store = Objects.requireNonNull(bandwidthsStore);
    }

    Bandwidth getOrCreateBandwidth(K key, Rate rate) {
        Bandwidth existing = getBandwidthFromStore(key); // Coming from store without auto save
        if (existing == null) {
            return rateToBandwidthConverter.convert(rate);
        }
        return withAutoSave(key, existing);
    }

    Bandwidth getOrCreateBandwidth(K key, Rates rates) {
        Bandwidth existing = getBandwidthFromStore(key);
        if (existing == null) {
            return rateToBandwidthConverter.convert(rates);
        }
        return withAutoSave(key, existing);
    }

    private Bandwidth withAutoSave(K key, Bandwidth bandwidth) {
        return new BandwidthWrapper(bandwidth) {
            @Override public long reserveEarliestAvailable(int permits, long nowMicros) {
                final long result = super.reserveEarliestAvailable(permits, nowMicros);
                BandwidthStoreFacade.this.saveBandwidthToStore(key, bandwidth);
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
