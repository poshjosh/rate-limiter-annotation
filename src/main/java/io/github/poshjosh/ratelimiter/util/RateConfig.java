package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.annotation.RateSource;

import java.util.Objects;

public final class RateConfig {

    public static RateConfig of(Rates value) {
        return new RateConfig(RateSource.NONE, value);
    }

    public static RateConfig of(RateSource source, Rates value) {
        return new RateConfig(source, value);
    }

    private final RateSource source;
    private final Rates rates;

    private RateConfig(RateSource source, Rates rates) {
        this.source = Objects.requireNonNull(source);
        this.rates = Objects.requireNonNull(rates);
    }

    public RateConfig withSource(RateSource source) {
        return RateConfig.of(source, rates);
    }

    public RateSource getSource() {
        return source;
    }

    public Rates getRates() {
        return rates;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RateConfig that = (RateConfig) o;
        return source.equals(that.source) && rates.equals(that.rates);
    }

    @Override public int hashCode() {
        return Objects.hash(source, rates);
    }

    @Override public String toString() {
        return "RateConfig{source=" + source.getClass().getSimpleName() + ", rates=" + rates + '}';
    }
}
