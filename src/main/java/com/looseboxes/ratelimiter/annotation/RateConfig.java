package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.util.Rates;

import java.util.Objects;

public final class RateConfig {

    public static  RateConfig of(Object source, Rates value) {
        return new RateConfig(source, value);
    }

    private final Object source;
    private final Rates value;

    private RateConfig(Object source, Rates value) {
        this.source = Objects.requireNonNull(source);
        this.value = Objects.requireNonNull(value);
    }

    public Object getSource() {
        return source;
    }

    public Rates getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateConfig rateConfig = (RateConfig) o;
        return source.equals(rateConfig.source) && value.equals(rateConfig.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, value);
    }

    @Override public String toString() {
        return "RateConfig{" + "source=" + source.getClass().getSimpleName() + ", value=" + value + '}';
    }
}
