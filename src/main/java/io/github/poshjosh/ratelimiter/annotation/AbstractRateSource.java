package io.github.poshjosh.ratelimiter.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

abstract class AbstractRateSource implements RateSource{

    private static final RateProcessor.SourceFilter isRateLimited = RateProcessor.SourceFilter.ofRateLimited();

    static final class ClassRateSource extends AbstractRateSource {
        private final String id;
        private final Class<?> clazz;
        private final boolean rateLimited;
        ClassRateSource(Class<?> clazz) {
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

    static final class MethodRateSource extends AbstractRateSource {
        private final String id;
        private final Method method;
        private final RateSource declarer;
        private final boolean rateLimited;
        MethodRateSource(Method method) {
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

    static final class GroupRateSource extends AbstractRateSource {
        private final String id;
        private final GenericDeclaration source;
        GroupRateSource(String id, GenericDeclaration source) {
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

    static final class None extends AbstractRateSource {
        private final String id;
        None(String id) {
            this.id = Objects.requireNonNull(id);
        }
        @Override public Object getSource() { return this; }
        @Override public String getId() { return id; }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> cls) {
            return Optional.empty();
        }
        @Override public boolean isRateLimited() { return false; }
    }
    @Override public int hashCode() { return Objects.hashCode(getId()); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractRateSource)) {
            return false;
        }
        return getId().equals(((AbstractRateSource)o).getId());
    }
    @Override public String toString() {
        return this.getClass().getSimpleName() + '{' + getId() + '}';
    }
}
