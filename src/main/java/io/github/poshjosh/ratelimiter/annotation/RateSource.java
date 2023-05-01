package io.github.poshjosh.ratelimiter.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * An source of rate limiting information. E.g a class, a method etc
 */
public interface RateSource {
    static RateSource of(Class<?> clazz) {
        return new AbstractRateSource.ClassRateSource(clazz);
    }

    static RateSource of(Method method) {
        return new AbstractRateSource.MethodRateSource(method);
    }

    static RateSource of(String id, GenericDeclaration source) {
        return new AbstractRateSource.GroupRateSource(id, source);
    }

    static RateSource of(String id) {
        return new AbstractRateSource.None(id);
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
