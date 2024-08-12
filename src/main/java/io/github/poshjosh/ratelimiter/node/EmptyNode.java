package io.github.poshjosh.ratelimiter.node;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class EmptyNode<V> implements Node<V>{

    EmptyNode() {}

    @Override
    public int size() {
        return 1;
    }

    @Override
    public void visitAll(Predicate<Node<V>> filter, Consumer<Node<V>> consumer, int depth) {
        if (depth > 0 && filter.test(this)) {
            consumer.accept(this);
        }
    }

    @Override
    public Node<V> findFirstOrDefault(
            Node<V> offset, Predicate<Node<V>> nodeTest, Node<V> resultIfNone) {
        return resultIfNone;
    }

    public boolean hasChildren() { return false; }

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

    @Override public String toString() {
        return "EmptyNode{}";
    }
}
