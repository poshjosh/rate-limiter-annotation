package com.looseboxes.ratelimiter.annotation;

import java.io.Serializable;
import java.util.Objects;

public final class NodeValue<V> implements Serializable {

    private static final long serialVersionUID = 40L;

    public static <T> NodeValue<T> of(Object source, T value) {
        return new NodeValue<>(source, value);
    }

    private final Object source;
    private final V value;

    private NodeValue(Object source, V value) {
        this.source = Objects.requireNonNull(source);
        this.value = Objects.requireNonNull(value);
    }

    public <T> NodeValue<T> withValue(T value) {
        return new NodeValue<>(this.source, value);
    }

    public Object getSource() {
        return source;
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeValue nodeValue = (NodeValue) o;
        return source.equals(nodeValue.source) && value.equals(nodeValue.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, value);
    }

    @Override public String toString() {
        return "NodeValue{" + "source=" + source.getClass().getSimpleName() + ", value=" + value + '}';
    }
}
