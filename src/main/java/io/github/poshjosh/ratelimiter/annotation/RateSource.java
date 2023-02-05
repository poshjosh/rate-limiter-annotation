package io.github.poshjosh.ratelimiter.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * An source of rate limiting information. E.g a class, a method etc
 */
public abstract class RateSource {

    public static final RateSource NONE = new RateSource() {
        @Override public Object getSource() { return this; }
        @Override public String getId() { return ""; }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> cls) {
            return Optional.empty();
        }
        @Override public boolean isRateLimited() { return false; }
        @Override public String toString() { return "RateSource$NONE"; }
    };

    private static final RateProcessor.SourceFilter isRateLimited = RateProcessor.SourceFilter.ofRateLimited();

    private static final class ClassRateSource extends RateSource {
        private final String id;
        private final Class<?> clazz;
        private final boolean rateLimited;
        private ClassRateSource(Class<?> clazz) {
            this.id = ElementId.of(clazz);
            this.clazz = clazz;
            this.rateLimited = isRateLimited.test(clazz);
        }
        @Override public Class<?> getSource() { return clazz; }
        @Override public Optional<RateSource> getDeclarer() {
            return Optional.of(this);
        }
        @Override public String getId() {
            return id;
        }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.ofNullable(clazz.getAnnotation(annotationClass));
        }
        @Override public boolean isRateLimited() { return rateLimited; }
    }

    private static final class MethodRateSource extends RateSource {
        private final String id;
        private final Method method;
        private final RateSource declarer;
        private final boolean rateLimited;
        private MethodRateSource(Method method) {
            this.id = ElementId.of(method);
            this.method = method;
            this.declarer = RateSource.of(method.getDeclaringClass());
            this.rateLimited = isRateLimited.test(method);
        }
        @Override public Method getSource() { return method; }
        @Override public Optional<RateSource> getDeclarer() { return Optional.of(declarer); }
        @Override public String getId() {
            return id;
        }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.ofNullable(method.getAnnotation(annotationClass));
        }
        @Override public boolean isRateLimited() { return rateLimited; }
    }

    private static final class GroupRateSource extends RateSource {
        private final String id;
        private final GenericDeclaration source;
        private GroupRateSource(String id, GenericDeclaration source) {
            this.id = Objects.requireNonNull(id);
            this.source = Objects.requireNonNull(source);
        }
        @Override public GenericDeclaration getSource() { return source; }
        @Override public String getId() {
            return id;
        }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.empty();
        }
        @Override public boolean isRateLimited() { return true; }
        @Override public boolean isGroupType() { return true; }
    }

    public static RateSource of(Class<?> clazz) {
        return new ClassRateSource(clazz);
    }

    public static RateSource of(Method method) {
        return new MethodRateSource(method);
    }

    public static RateSource of(String id, GenericDeclaration source) {
        return new GroupRateSource(id, source);
    }

    public abstract Object getSource();
    public abstract String getId();
    public abstract <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass);
    public abstract boolean isRateLimited();

    public boolean isGroupType() { return false; }
    public boolean isOwnDeclarer() {
        return getDeclarer().orElse(null) == this;
    }
    public Optional<RateSource> getDeclarer() { return Optional.empty(); }

    @Override public int hashCode() { return Objects.hashCode(getId()); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return getId().equals(((RateSource)o).getId());
    }
    @Override public String toString() {
        return this.getClass().getSimpleName() + '{' + getId() + '}';
    }
}
