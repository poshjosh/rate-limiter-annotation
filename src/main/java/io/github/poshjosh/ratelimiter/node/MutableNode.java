package io.github.poshjosh.ratelimiter.node;

import java.util.List;

public interface MutableNode<V> extends Node<V> {
    boolean addChild(MutableNode<V> child);
    MutableNode<V> removeChild(String name);
    List<MutableNode<V>> getChildren();

    /**
     * Get leaf child nodes (excluding root node, or {@link Nodes#EMPTY}).
     * @return leaf child nodes (excluding root node, or {@link Nodes#EMPTY})..
     * @see #isLeaf()
     * @see #isRoot()
     * @see #isEmptyNode()
     */
    Node<V>[] collectLeafs();

    /**
     * Get leaf child nodes, earlier collected via method {@link #collectLeafs()}, or fail.
     * @return leaf child nodes, earlier collected via method {@link #collectLeafs()}, or fail.
     * @see #collectLeafs()
     * @throws UnsupportedOperationException if method {@link #collectLeafs()}
     * was not previously called.
     */
    Node<V>[] getCollectedLeafs() throws UnsupportedOperationException;
}
