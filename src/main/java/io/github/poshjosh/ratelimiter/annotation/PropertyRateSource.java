package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;

public final class PropertyRateSource implements RateSource {

    public static RateSource of(RateLimitProperties source, Rates rates) {
        return new PropertyRateSource(source, rates);
    }

    private final Rates rates;

    private final RateLimitProperties source;

    private PropertyRateSource(RateLimitProperties source, Rates rates) {
        this.rates = Objects.requireNonNull(rates);
        this.source = Objects.requireNonNull(source);
    }

    @Override public String getId() {
        return rates.getId();
    }

    @Override public Object getSource() {
        return source;
    }

    @Override public Rates getRates() {
        return rates;
    }

    @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
        return Optional.empty();
    }

    @Override public boolean isRateLimited() {
        return rates.isSet();
    }

    @Override public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PropertyRateSource)) {
            return false;
        }
        return getId().equals(((PropertyRateSource) o).getId());
    }

    @Override public String toString() {
        return this.getClass().getSimpleName() + '{' + getId() + '}';
    }
}
