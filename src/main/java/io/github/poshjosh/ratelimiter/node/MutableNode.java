package io.github.poshjosh.ratelimiter.node;

public interface MutableNode<V> extends Node<V> {
    boolean addChild(Node<V> child);
}
