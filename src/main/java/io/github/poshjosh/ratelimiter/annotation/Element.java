package io.github.poshjosh.ratelimiter.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * An element that may be annotated. e.g a class or a method.
 */
public abstract class Element {

    static Element of(Class<?> element) {
        Objects.requireNonNull(element);
        final String id = ElementId.of(element);
        return new Element() {
            @Override public Element getDeclarer() {
                return this;
            }
            @Override public String getId() {
                return id;
            }
            @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
                return Optional.ofNullable(element.getAnnotation(annotationClass));
            }
        };
    }

    static Element of(Method element) {
        return of(element, Element.of(element.getDeclaringClass()));
    }

    static Element of(Method element, Element declaringElement) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(declaringElement);
        final String id = ElementId.of(element);
        return new Element() {
            @Override public Element getDeclarer() {
                return declaringElement;
            }
            @Override public String getId() {
                return id;
            }
            @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
                return Optional.ofNullable(element.getAnnotation(annotationClass));
            }
        };
    }

    static Element of(String id) {
        Objects.requireNonNull(id);
        return new Element() {
            @Override public Element getDeclarer() {
                return this;
            }
            @Override public String getId() {
                return id;
            }
            @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
                return Optional.empty();
            }
        };
    }

    public abstract Element getDeclarer();
    public abstract String getId();
    public abstract <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass);

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
