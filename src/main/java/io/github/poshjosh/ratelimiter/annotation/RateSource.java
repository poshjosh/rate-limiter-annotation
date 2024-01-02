package io.github.poshjosh.ratelimiter.annotation;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;

/**
 * A source of rate limiting information. E.g a class, a method etc
 */
public interface RateSource {

    static RateSource of(String id, boolean isRateLimited) {
        return new RateSource() {
            @Override public Object getSource() { return this; }
            @Override public String getId() { return id; }
            @Override public <T extends Annotation> Optional<T> getAnnotation(
                    Class<T> annotationClass) {
                return Optional.empty();
            }
            @Override public boolean isRateLimited() { return isRateLimited; }
            @Override public int hashCode() { return Objects.hashCode(getId()); }
            @Override public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof RateSource)) {
                    return false;
                }
                return getId().equals(((RateSource)o).getId());
            }
            @Override public String toString() {
                return this.getClass().getSimpleName() + '{' + getId() + '}';
            }
        };
    }

    Object getSource();
    String getId();
    <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass);
    boolean isRateLimited();
    default boolean isGroupType() { return false; }
    default boolean isOwnDeclarer() {
        return getDeclarer().orElse(null) == this;
    }
    default Optional<RateSource> getDeclarer() { return Optional.empty(); }
}
