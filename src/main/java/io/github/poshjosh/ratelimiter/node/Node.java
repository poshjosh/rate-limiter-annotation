/*
 * Copyright 2017 NUROX Ltd.
 *
 * Licensed under the NUROX Ltd Software License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.looseboxes.com/legal/licenses/software.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.poshjosh.ratelimiter.node;

import io.github.poshjosh.ratelimiter.annotation.exceptions.NodeValueAbsentException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

/**
 * @author Chinomso Bassey Ikwuagwu on Oct 13, 2017 2:58:54 PM
 * @param <V> The type of the value returned by this node
 */
public interface Node<V> {
    static <T> Node<T> empty() {
        return Nodes.empty();
    }

    default boolean anyMatch(Predicate<Node<V>> test) {
        return test.test(this) || getChildren().stream().anyMatch(child -> child.anyMatch(test));
    }

    default int size() {
        AtomicInteger sum = new AtomicInteger();
        Consumer<Node<V>> consumer = e -> sum.incrementAndGet();
        this.visitAll(consumer);
        return sum.get();
    }

    default List<Node<V>> getSiblings() {
        Node<V> parent = getParentOrDefault(null);
        if (parent == null) { // is root
            return Collections.emptyList();
        }
        return parent.getChildren();
    }

    default void visitAll(Consumer<Node<V>> consumer) {
        visitAll(node -> true, consumer);
    }

    default void visitAll(Predicate<Node<V>> filter, Consumer<Node<V>> consumer) {
        visitAll(filter, consumer, Integer.MAX_VALUE);
    }

    void visitAll(Predicate<Node<V>> filter, Consumer<Node<V>> consumer, int depth);

    /**
     * Copy this node and it's children onto the specified parent.
     *
     * The value of this node is not deep copied. To achieve deep copy use the transform method.
     * Use that method's value converter to create a deep copy of the node's value.
     *
     * @param parent The parent to copy this node and it's children to
     * @return The copy version of this node
     * @throws StackOverflowError If the Node calling this method is passed in as the parent argument
     * @see #transform(Node, Function)
     */
    default Node<V> copyTo(Node<V> parent) {
        return transform(parent, node -> node.getValueOrDefault(null));
    }

    /**
     * Retain all nodes (this node inclusive) which pass the test
     * @see #transform(Predicate, Node, Function, Function)
     */
    default Optional<Node<V>> retainAll(Predicate<Node<V>> test) {
        return transform(test, node -> node.getValueOrDefault(null));
    }

    /**
     * Copy a transformed version of this node and it's children onto the specified parent.
     * @see #transform(Predicate, Node, Function, Function)
     */
    default <T> Node<T> transform(Function<Node<V>, T> valueConverter) {
        return transform((Predicate<Node<V>>)(node -> true), valueConverter)
                .orElseThrow(() -> new AssertionError("Should not happen"));
    }

    /**
     * Copy a transformed version of this node and it's children onto the specified parent.
     * @see #transform(Predicate, Node, Function, Function)
     */
    default <T> Optional<Node<T>> transform(Predicate<Node<V>> test, Function<Node<V>, T> valueConverter) {
        return transform(test, null, valueConverter);
    }

    /**
     * Copy a transformed version of this node and it's children onto the specified parent.
     * @see #transform(Predicate, Node, Function, Function)
     */
    default <T> Node<T> transform(Node<T> newParent, Function<Node<V>, T> valueConverter) {
        return transform(node -> true, newParent, valueConverter)
                .orElseThrow(() -> new AssertionError("Should not happen"));
    }

    /**
     * Copy a transformed version of this node and it's children onto the specified parent.
     * @see #transform(Predicate, Node, Function, Function)
     */
    default <T> Optional<Node<T>> transform(Predicate<Node<V>> test, Node<T> newParent, Function<Node<V>, T> valueConverter) {
        return transform(test, newParent, Node::getName, valueConverter);
    }

    /**
     * Copy a transformed version of this node and it's children onto the specified parent.
     * @see #transform(Predicate, Node, Function, Function)
     */
    default <T> Node<T> transform(Function<Node<V>, String> nameConverter, Function<Node<V>, T> valueConverter) {
        return transform(Nodes.of(getName()), nameConverter, valueConverter);
    }

    /**
     * Copy a transformed version of this node and it's children onto the specified parent.
     * @see #transform(Predicate, Node, Function, Function)
     */
    default <T> Node<T> transform(Node<T> newParent, Function<Node<V>, String> nameConverter, Function<Node<V>, T> valueConverter) {
        return transform(node -> true, newParent, nameConverter, valueConverter)
                .orElseThrow(() -> new AssertionError("Should not happen"));
    }

    /**
     * Copy a transformed version of this node and it's children onto the specified parent.
     *
     * @param <T> The type of the value of the transformed copy
     * @param test Only nodes that pass this test will be accepted to the transformed tree
     * @param newParent The parent to copy a transformed version of this node and it's children to
     * @param nameConverter The converter which will be applied to produce a new name for each node in this tree
     * @param valueConverter The converter which will be applied to produce a new value for each node in this tree
     * @return The transformed copy of this node
     * @throws StackOverflowError If the Node calling this method is passed in as the newParent argument
     * @see #transform(Node, Function)
     */
    default <T> Optional<Node<T>> transform(Predicate<Node<V>> test, Node<T> newParent,
            Function<Node<V>, String> nameConverter, Function<Node<V>, T> valueConverter) {
        if (!test.test(this)) {
            return Optional.empty();
        }
        final String newName = nameConverter.apply(this);
        final T newValue = valueConverter.apply(this);
        final Node<T> newNode = Nodes.of(newName, newValue, newParent);
        getChildren().forEach(child -> child.transform(test, newNode, nameConverter, valueConverter));
        return Optional.of(newNode);
    }

    default boolean isEmptyNode() {
        return this == Nodes.EMPTY;
    }

    default boolean isRoot() {
        return getParentOrDefault(null) == null;
    }

    default boolean isLeaf() {
        return !hasChildren();
    }
        
    /**
     * The root node is the only node at level <code>Zero (0)</code>
     * The direct children of the root node are at level <code>One (0)</code>
     * and so on and forth
     * @return The level of this node
     * @see #getRoot() 
     */
    default int getLevel() {
        final Node<V> parent = this.getParentOrDefault(null);
        if (parent != null) {
            return parent.getLevel() + 1;
        }else{
            return 0;
        }        
    }

    /**
     * @return The topmost <tt>parent node</tt> in this node's heirarchy.
     */
    default Node<V> getRoot() {
        if (isRoot()) {
            return this;
        }
        Node<V> target = this;
        while(target.getParentOrDefault(null) != null) {
            target = target.getParentOrDefault(null);
        }
        return target;
    }

    default Optional<Node<V>> findFirstChild() {
        return findFirstChild(node -> true);
    }

    default Optional<Node<V>> findFirstChild(Predicate<Node<V>> nodeTest) {
        
        return this.findFirst(this, nodeTest);
    }

    Optional<Node<V>> findFirst(Node<V> offset, Predicate<Node<V>> nodeTest);

    boolean hasChildren();

    Node<V> getChild(int index);

    default int getChildCount() {
        return getChildren().size();
    }

    /**
     * @return An <b>un-modifiable</b> list view of this node's children
     */
    List<Node<V>> getChildren();

    String getName();

    default boolean hasValue() {
        return getValueOrDefault(null) != null;
    }

    default V requireValue() {
        final V value = getValueOrDefault(null);
        if (value == null) {
            throw new NodeValueAbsentException(this);
        }
        return value;
    }

    default Optional<V> getValueOptional() {
        return Optional.ofNullable(this.getValueOrDefault(null));
    }

    V getValueOrDefault(V outputIfNone);

    default boolean hasParent() {
        return getParentOrDefault(null) != null;
    }

    default Optional<Node<V>> getParentOptional() {
        return Optional.ofNullable(this.getParentOrDefault(null));
    }
    
    Node<V> getParentOrDefault(Node<V> outputIfNone);
}
