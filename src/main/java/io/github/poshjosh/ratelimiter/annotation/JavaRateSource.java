package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.model.RateSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

public final class JavaRateSource {
    private JavaRateSource() { }

    public static RateSource of(GenericDeclaration source) {
        if (source instanceof Class) {
            return of((Class)source);
        }
        if (source instanceof Method) {
            return of((Method)source);
        }
        throw new UnsupportedOperationException("Unsupported source type: " + source);
    }

    public static RateSource of(Class<?> clazz) {
        if (clazz.isAnnotation()) {
            return ofAnnotation(clazz);
        }
        return new ClassRateSource(clazz);
    }

    public static RateSource of(Method method) {
        return new MethodRateSource(method);
    }

    public static RateSource ofAnnotation(Class<?> source) {
        if (!source.isAnnotation()) {
            throw new IllegalArgumentException("Source must be an annotation type");
        }
        return new AnnotationRateSource(source);
    }

    private static final RateProcessor.SourceFilter isRateLimited =
            RateProcessor.SourceFilter.ofRateLimited();

    private static final class ClassRateSource extends AbstractRateSource {
        private final String id;
        private final Class<?> clazz;
        private final boolean rateLimited;
        private ClassRateSource(Class<?> clazz) {
            this.id = RateId.of(clazz);
            this.clazz = Objects.requireNonNull(clazz);
            this.rateLimited = isRateLimited.test(clazz);
        }
        @Override public String getId() {
            return id;
        }
        @Override public Class<?> getSource() { return clazz; }
        @Override public Optional<RateSource> getDeclarer() {
            return Optional.of(this);
        }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.ofNullable(clazz.getAnnotation(annotationClass));
        }
        @Override public boolean isRateLimited() { return rateLimited; }
    }

    private static final class MethodRateSource extends AbstractRateSource {
        private final String id;
        private final Method method;
        private final RateSource declarer;
        private final boolean rateLimited;
        private MethodRateSource(Method method) {
            this.id = RateId.of(method);
            this.method = Objects.requireNonNull(method);
            this.declarer = of(method.getDeclaringClass());
            this.rateLimited = isRateLimited.test(method);
        }
        @Override public String getId() {
            return id;
        }
        @Override public Method getSource() { return method; }
        @Override public Optional<RateSource> getDeclarer() { return Optional.of(declarer); }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.ofNullable(method.getAnnotation(annotationClass));
        }
        @Override public boolean isRateLimited() { return rateLimited; }
    }

    private static final class AnnotationRateSource extends AbstractRateSource {
        private final String id;
        private final Class<?> clazz;
        private final boolean rateLimited;
        private AnnotationRateSource(Class<?> source) {
            this.id = RateId.of(source);
            this.clazz = Objects.requireNonNull(source);
            this.rateLimited = isRateLimited.test(source);
        }

        @Override public String getId() {
            return id;
        }
        @Override public GenericDeclaration getSource() { return clazz; }

        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.ofNullable(clazz.getAnnotation(annotationClass));
        }
        @Override public boolean isRateLimited() { return rateLimited; }
        @Override public boolean isGroupType() { return true; }
    }

    protected abstract static class AbstractRateSource implements RateSource {
        protected AbstractRateSource() {}
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
    }
}
