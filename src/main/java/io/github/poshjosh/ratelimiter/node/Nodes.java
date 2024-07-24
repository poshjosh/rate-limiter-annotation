package io.github.poshjosh.ratelimiter.node;

public interface Nodes {
    Node<Object> EMPTY = new EmptyNode<>();

    @SuppressWarnings("unchecked")
    static <T> Node<T> empty() {
        return (Node<T>)EMPTY;
    }

    static <T> Node<T> ofDefaultRoot() {
        return of("root");
    }

    static <T> Node<T> of(String name) {
        return of(name, null);
    }

    static <T> Node<T> of(String name, T value) {
        return of(name, value, null);
    }

    static <T> Node<T> of(String name, Node<T> parent) {
        return of(name, null, parent);
    }

    static <T> Node<T> ofDefaultParent(String name, T value) { return of(name, value, ofDefaultRoot()); }

    static <T> Node<T> of(String name, T value, Node<T> parent) {
        return new NodeImpl(name, value, parent);
    }
}
