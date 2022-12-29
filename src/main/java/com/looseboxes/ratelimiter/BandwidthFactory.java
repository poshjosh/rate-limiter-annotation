package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.bandwidths.Bandwidth;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.looseboxes.ratelimiter.BandwidthFactories.getOrCreateBandwidthFactory;

/**
 * A factory type for creating {@link Bandwidth}s.
 *
 * Implementations are required to have a no-argument constructor.
 */
public interface BandwidthFactory {

    final class Default implements BandwidthFactory {
        // This is initialized from a system property. We want to use the value at start up and not change
        // so we do static initialization.
        private static final BandwidthFactory delegate = BandwidthFactories.createSystemBandwidthFactory();
        public Default() { }
        @Override
        public Bandwidth createNew(long permits, long duration, TimeUnit timeUnit, long nowMicros) {
            return delegate.createNew(permits, duration, timeUnit, nowMicros);
        }
        @Override
        public String toString() {
            return "BandwidthFactory$Default{delegate=" + delegate + "}";
        }
    }

    final class SmoothBursty implements BandwidthFactory {
        private final double maxBurstsSeconds;
        public SmoothBursty() {
            this(1.0);
        }
        public SmoothBursty(double maxBurstsSeconds) {
            this.maxBurstsSeconds = maxBurstsSeconds;
        }
        @Override
        public Bandwidth createNew(long permits, long duration, TimeUnit timeUnit, long nowMicros) {
            return createNew(SmoothWarmingUp.toPermitsPerSecond(permits, duration, timeUnit), nowMicros);
        }
        private Bandwidth createNew(double permitsPerSecond, long nowMicros) {
            return Bandwidth.bursty(permitsPerSecond, nowMicros, maxBurstsSeconds);
        }
        @Override
        public String toString() { return "BandwidthFactory$SmoothBursty{maxBurstsSeconds=" + maxBurstsSeconds + '}'; }
    }

    final class SmoothWarmingUp implements BandwidthFactory {
        private final long warmupPeriod;
        private final TimeUnit timeUnit;
        private final double coldFactor;
        public SmoothWarmingUp() {
            this(1, TimeUnit.SECONDS, 3.0);
        }
        public SmoothWarmingUp(long warmupPeriod, TimeUnit timeUnit, double coldFactor) {
            this.warmupPeriod = warmupPeriod;
            this.timeUnit = Objects.requireNonNull(timeUnit);
            this.coldFactor = coldFactor;
        }
        @Override
        public Bandwidth createNew(long permits, long duration, TimeUnit timeUnit, long nowMicros) {
            return createNew(toPermitsPerSecond(permits, duration, timeUnit), nowMicros);
        }
        private Bandwidth createNew(double permitsPerSecond, long nowMicros) {
            return Bandwidth.warmingUp(permitsPerSecond, nowMicros, warmupPeriod, timeUnit, coldFactor);
        }
        private static double toPermitsPerSecond(final long amount, final long duration, final TimeUnit timeUnit) {
            // We use the highest precision
            final long nanosDuration = timeUnit.toNanos(duration);
            final double perNanos = (double)amount / nanosDuration;
            // Won't work because it will return zero if the result is a fraction
            //SECONDS.convert((long)perNanos, NANOSECONDS);
            return perNanos * TimeUnit.SECONDS.toNanos(1L);
        }
        @Override
        public String toString() {
            return "BandwidthFactory$SmoothWarmingUp{warmupPeriod=" + warmupPeriod +
                    ", timeUnit=" + timeUnit + ", coldFactor=" + coldFactor + '}';
        }
    }

    /** Beta */
    final class AllOrNothing implements BandwidthFactory {
        @Override
        public Bandwidth createNew(long permits, long duration, TimeUnit timeUnit, long nowMicros) {
            return Bandwidth.allOrNothing(permits, duration, timeUnit, nowMicros);
        }
        @Override
        public String toString() {
            return "BandwidthFactory$AllOrNothing{}";
        }
    }

    static BandwidthFactory getDefault() {
        return getOrCreateBandwidthFactory(BandwidthFactory.Default.class);
    }

    static BandwidthFactory bursty() {
        return getOrCreateBandwidthFactory(SmoothBursty.class);
    }

    static BandwidthFactory bursty(double maxBurstsSeconds) {
        return new SmoothBursty(maxBurstsSeconds);
    }

    static BandwidthFactory warmingUp() { return getOrCreateBandwidthFactory(SmoothWarmingUp.class); }

    static BandwidthFactory warmingUp(long warmupPeriod, TimeUnit timeUnit, double coldFactor) {
        return new SmoothWarmingUp(warmupPeriod, timeUnit, coldFactor);
    }

    /** Beta */
    static BandwidthFactory allOrNothing() {
        return getOrCreateBandwidthFactory(AllOrNothing.class);
    }

    Bandwidth createNew(long permits, long duration, TimeUnit timeUnit, long nowMicros);

    default Bandwidth createNew(long permits, Duration duration) {
        return createNew(permits, duration.toNanos(), TimeUnit.NANOSECONDS);
    }
    default Bandwidth createNew(long permits, Duration duration, long nowMicros) {
        return createNew(permits, duration.toNanos(), TimeUnit.NANOSECONDS, nowMicros);
    }

    default Bandwidth createNew(long permits, long duration, TimeUnit timeUnit) {
        return createNew(permits, duration, timeUnit, 0);
    }
}
