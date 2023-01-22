package io.github.poshjosh.ratelimiter.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * An element that may be annotated. e.g a class or a method.
 */
public abstract class Element {

    private static final RateProcessor.SourceFilter isRateLimited = RateProcessor.SourceFilter.ofRateLimited();

    private static final class ClassElement extends Element {
        private final String id;
        private final Class<?> clazz;
        private final boolean rateLimited;
        private ClassElement(Class<?> clazz) {
            this.id = ElementId.of(clazz);
            this.clazz = clazz;
            this.rateLimited = isRateLimited.test(clazz);
        }
        @Override public Element getDeclarer() {
            return this;
        }
        @Override public String getId() {
            return id;
        }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.ofNullable(clazz.getAnnotation(annotationClass));
        }
        @Override public boolean isRateLimited() { return rateLimited; }
    }

    private static final class MethodElement extends Element {
        private final String id;
        private final Method method;
        private final Element declarer;
        private final boolean rateLimited;
        private MethodElement(Method method) {
            this.id = ElementId.of(method);
            this.method = method;
            this.declarer = Element.of(method.getDeclaringClass());
            this.rateLimited = isRateLimited.test(method);
        }
        @Override public Element getDeclarer() { return declarer; }
        @Override public String getId() {
            return id;
        }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.ofNullable(method.getAnnotation(annotationClass));
        }
        @Override public boolean isRateLimited() { return rateLimited; }
    }

    private static final class GroupElement extends Element{
        private final String id;
        private GroupElement(String id) {
            this.id = Objects.requireNonNull(id);
        }
        @Override public Element getDeclarer() {
            return this;
        }
        @Override public String getId() {
            return id;
        }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.empty();
        }
        @Override public boolean isRateLimited() { return true; }
    }

    static Element of(Class<?> clazz) {
        return new ClassElement(clazz);
    }

    static Element of(Method method) {
        return new MethodElement(method);
    }

    static Element of(String id) {
        return new GroupElement(id);
    }

    public abstract Element getDeclarer();
    public abstract String getId();
    public abstract <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass);
    public abstract boolean isRateLimited();

    public boolean isOwnDeclarer() {
        return getDeclarer() == this;
    }

    @Override public int hashCode() { return Objects.hashCode(getId()); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return getId().equals(((Element)o).getId());
    }
    @Override public String toString() { return getId(); }
}
