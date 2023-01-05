package com.looseboxes.ratelimiter.util;

import com.looseboxes.ratelimiter.BandwidthFactory;
import com.looseboxes.ratelimiter.Checks;

import java.time.Duration;
import java.util.Objects;

public final class Rate {

    public static Rate ofNanos(long permits) {
        return of(permits, Duration.ofNanos(1));
    }

    public static Rate ofMillis(long permits) {
        return of(permits, Duration.ofMillis(1));
    }

    public static Rate ofSeconds(long permits) {
        return of(permits, Duration.ofSeconds(1));
    }

    public static Rate ofMinutes(long permits) {
        return of(permits, Duration.ofMinutes(1));
    }

    public static Rate ofHours(long permits) {
        return of(permits, Duration.ofHours(1));
    }

    public static Rate ofDays(long permits) {
        return of(permits, Duration.ofDays(1));
    }

    public static Rate of(long permits, Duration duration) {
        return of(permits, duration, BandwidthFactory.Default.class);
    }

    public static Rate of(long permits, Duration duration, Class<? extends BandwidthFactory> factoryClass) {
        return new Rate(permits, duration, factoryClass);
    }

    public static Rate of(Rate rate) {
        return new Rate(rate);
    }

    private long permits;
    private Duration duration = Duration.ofSeconds(1);

    /**
     * A {@link BandwidthFactory} that will be dynamically instantiated and used to create
     * {@link com.looseboxes.ratelimiter.bandwidths.Bandwidth}s from this rate limit.
     * The class must have a zero-argument constructor.
     */
    private Class<? extends BandwidthFactory> factoryClass = BandwidthFactory.Default.class;

    public Rate() { }

    Rate(Rate rate) {
        this(rate.permits, rate.duration, rate.factoryClass);
    }

    Rate(long permits, Duration duration, Class<? extends BandwidthFactory> factoryClass) {
        Checks.requireNotNegative(permits, "permits");
        Checks.requireFalse(duration.isNegative(), "Duration must be positive, duration: " + duration);
        this.permits = permits;
        this.duration = Objects.requireNonNull(duration);
        this.factoryClass = Objects.requireNonNull(factoryClass);
    }

    public boolean isSet() {
        return permits > 0 && duration != null;
    }

    public Rate permits(long permits) {
        this.setPermits(permits);
        return this;
    }

    public long getPermits() {
        return permits;
    }

    public void setPermits(long permits) {
        this.permits = permits;
    }

    public Rate duration(Duration duration) {
        this.setDuration(duration);
        return this;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public Rate factoryClass(Class<? extends BandwidthFactory> factoryClass) {
        setFactoryClass(factoryClass);
        return this;
    }

    public Class<? extends BandwidthFactory> getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(Class<? extends BandwidthFactory> factoryClass) {
        this.factoryClass = factoryClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rate that = (Rate) o;
        return permits == that.permits && duration.equals(that.duration) && factoryClass.equals(that.factoryClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permits, duration, factoryClass);
    }

    @Override
    public String toString() {
        return "Rate{" +
                "permits=" + permits +
                ", duration=" + duration +
                ", factoryClass=" + (factoryClass == null ? null : factoryClass.getSimpleName()) +
                '}';
    }
}
