package com.looseboxes.ratelimiter.node;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class EmptyNode<V> implements Node<V>{

    EmptyNode() {}

    @Override
    public void visitAll(Consumer<Node<V>> consumer) {}

    @Override
    public <T> Node<T> transform(Node<T> newParent, BiFunction<String, V, String> nameConverter, BiFunction<String, V, T> valueConverter) {
        return null;
    }

    @Override
    public Optional<Node<V>> findFirst(Node<V> offset, V... path) {
        return Optional.empty();
    }

    @Override
    public Optional<Node<V>> findFirst(Node<V> offset, Predicate<Node<V>> nodeTest) {
        return Optional.empty();
    }

    @Override
    public Node<V> getChild(int index) {
        throw new IndexOutOfBoundsException("Index: " + index + ", size: 0");
    }

    @Override
    public List<Node<V>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public V getValueOrDefault(V outputIfNone) {
        return outputIfNone;
    }

    @Override
    public Node<V> getParentOrDefault(Node<V> outputIfNone) {
        return outputIfNone;
    }
}
