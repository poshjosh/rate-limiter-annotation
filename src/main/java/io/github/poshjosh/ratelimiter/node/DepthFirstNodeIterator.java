package io.github.poshjosh.ratelimiter.node;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class DepthFirstNodeIterator<V> implements Iterator<Node<V>> {

    private final Deque<Node<V>> stack = new ArrayDeque<>();

    DepthFirstNodeIterator(Node<V> root) {
        if (root != null) {
            stack.push(root);
        }
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public Node<V> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final Node<V> current = stack.pop();

        // Push children in reverse so left-most child is processed first
        final int childCount = current.getChildCount();
        if (childCount > 0) {
            for (int i = childCount - 1; i >= 0; i--) {
                stack.push(current.getChild(i));
            }
        }

        return current;
    }
}
