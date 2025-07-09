package io.github.poshjosh.ratelimiter.node;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
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
            Node<V> offset, Predicate<Node<V>> nodeTest, int depth, Node<V> resultIfNone) {
        if (depth <= 0) {
            return resultIfNone;
        }
        return offset == this && nodeTest.test(this) ? this : resultIfNone;
    }

    @Override
    public boolean anyChildMatch(Predicate<Node<V>> test) {
        return false;
    }

    @Override
    public <T> Optional<Node<T>> transform(Predicate<Node<V>> test, Node<T> newParent,
            Function<Node<V>, String> nameConverter, Function<Node<V>, T> valueConverter) {
        if (!test.test(this)) {
            return Optional.empty();
        }
        final String newName = nameConverter.apply(this);
        final T newValue = valueConverter.apply(this);
        final Node<T> newNode = Nodes.of(newName, newValue, newParent);
        return Optional.of(newNode);
    }

    @Override
    public int getChildCount() { return 0; }

    @Override
    public Node<V> getChild(int index) {
        throw new IndexOutOfBoundsException("Index: " + index + ", size: 0");
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

    @Override
    public Iterator<Node<V>> iterator() {
        return new Iterator<Node<V>>() {
            private Node<V> next = EmptyNode.this;
            @Override public boolean hasNext() {
                return next != null;
            }

            @Override public Node<V> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Node<V> result = next;
                next = null; // EmptyNode has no children, so we set next to null
                return result;
            }

            @Override public String toString() {
                return "EmptyNode.Iterator{" + next +"}";
            }
        };
    }

    @Override public String toString() {
        return "EmptyNode{}";
    }
}
