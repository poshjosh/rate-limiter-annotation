package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.annotation.Element;

import java.util.Objects;

public final class RateConfig {

    public static RateConfig of(Rates value) {
        return new RateConfig(value, value);
    }

    public static RateConfig of(Object source, Rates value) {
        return new RateConfig(source, value);
    }

    private final SourceType sourceType;
    private final Object source;
    private final Rates rates;

    private RateConfig(Object source, Rates rates) {
        this.sourceType = getType(source);
        this.source = Objects.requireNonNull(source);
        this.rates = Objects.requireNonNull(rates);
    }
    private SourceType getType(Object source) {
        if (!(source instanceof Element)) {
            return SourceType.PROPERTY;
        }
        Element element = (Element)source;
        if (element.isGroupType()) {
            return SourceType.GROUP;
        }
        if (element.isOwnDeclarer()) {
            return SourceType.CLASS;
        }
        return SourceType.METHOD;
    }

    public RateConfig withSource(Object source) {
        return RateConfig.of(source, rates);
    }

    public RateConfig withRates(Rates value) {
        return RateConfig.of(source, value);
    }

    public SourceType getSourceType() { return sourceType; }

    public Object getSource() {
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
        return sourceType == that.sourceType && source.equals(that.source) && rates.equals(that.rates);
    }

    @Override public int hashCode() {
        return Objects.hash(sourceType, source, rates);
    }

    @Override public String toString() {
        return "RateConfig{type=" + sourceType + ", source=" +
                source.getClass().getSimpleName() + ", rates=" + rates + '}';
    }
}
