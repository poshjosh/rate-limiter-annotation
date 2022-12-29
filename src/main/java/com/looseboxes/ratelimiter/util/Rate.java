package com.looseboxes.ratelimiter.util;

import com.looseboxes.ratelimiter.BandwidthFactory;
import com.looseboxes.ratelimiter.Checks;

import java.time.Duration;
import java.util.Objects;

public final class Rate {

    public static Rate of(long limit, Duration duration) {
        return of(limit, duration, BandwidthFactory.Default.class);
    }

    public static Rate of(long limit, Duration duration, Class<? extends BandwidthFactory> factoryClass) {
        return new Rate(limit, duration, factoryClass);
    }

    public static Rate of(Rate rate) {
        return new Rate(rate);
    }

    private long limit;
    private Duration duration;

    /**
     * A {@link BandwidthFactory} that will be dynamically instantiated and used to create
     * {@link com.looseboxes.ratelimiter.bandwidths.Bandwidth}s from this rate limit.
     * The class must have a zero-argument constructor.
     */
    private Class<? extends BandwidthFactory> factoryClass = BandwidthFactory.Default.class;

    public Rate() { }

    Rate(Rate rate) {
        this(rate.limit, rate.duration, rate.factoryClass);
    }

    Rate(long limit, Duration duration, Class<? extends BandwidthFactory> factoryClass) {
        Checks.requireNotNegative(limit, "limit");
        Checks.requireFalse(duration.isNegative(), "Duration must be positive, duration: " + duration);
        this.limit = limit;
        this.duration = Objects.requireNonNull(duration);
        this.factoryClass = Objects.requireNonNull(factoryClass);
    }

    public Rate limit(long limit) {
        this.setLimit(limit);
        return this;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
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
        return limit == that.limit && duration.equals(that.duration) && factoryClass.equals(that.factoryClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, duration, factoryClass);
    }

    @Override
    public String toString() {
        return "Rate{" +
                "limit=" + limit +
                ", duration=" + duration +
                ", factoryClass=" + (factoryClass == null ? null : factoryClass.getSimpleName()) +
                '}';
    }
}
