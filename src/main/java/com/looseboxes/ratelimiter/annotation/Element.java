package com.looseboxes.ratelimiter.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

public abstract class Element {

    static Element of(Class<?> element) {
        return of(ElementId.of(element), element);
    }

    static Element of(String id, Class<?> element) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(element);
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
        return of(ElementId.of(element), element);
    }

    static Element of(String id, Method element) {
        return of(id, element, Element.of(element.getDeclaringClass()));
    }

    static Element of(Method element, Element declaringElement) {
        return of(ElementId.of(element), element, declaringElement);
    }

    static Element of(String id, Method element, Element declaringElement) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(element);
        Objects.requireNonNull(declaringElement);
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

    public boolean isOwnDeclarer() {
        return getDeclarer() == this;
    }

    public abstract Element getDeclarer();
    public abstract String getId();
    public abstract <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass);

    @Override public int hashCode() { return Objects.hashCode(getId()); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return getId().equals(((Element)o).getId());
    }
    @Override public String toString() { return getId(); }
}
