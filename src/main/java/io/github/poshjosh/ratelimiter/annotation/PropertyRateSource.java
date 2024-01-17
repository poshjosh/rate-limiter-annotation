package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;

public final class PropertyRateSource implements RateSource {
    private final String id;

    private final boolean rateLimited;

    private final RateLimitProperties source;

    public PropertyRateSource(String id, boolean rateLimited, RateLimitProperties source) {
        this.id = Objects.requireNonNull(id);
        this.rateLimited = rateLimited;
        this.source = Objects.requireNonNull(source);
    }

    @Override public Object getSource() {
        return source;
    }

    @Override public String getId() {
        return id;
    }

    @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
        return Optional.empty();
    }

    @Override public boolean isRateLimited() {
        return rateLimited;
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
