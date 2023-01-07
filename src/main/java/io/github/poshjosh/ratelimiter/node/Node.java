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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Chinomso Bassey Ikwuagwu on Oct 13, 2017 2:58:54 PM
 * @param <V> The type of the value returned by this node
 */
public interface Node<V> {

    Node<Object> EMPTY = new EmptyNode<>();

    @SuppressWarnings("unchecked")
    static <T> Node<T> empty() {
        return (Node<T>)EMPTY;
    }

    static <T> Node<T> of(String name) {
        return of(name, null);
    }

    static <T> Node<T> of(String name, T value) {
        return of(name, value, null);
    }

    static <T> Node<T> of(String name, T value, Node<T> parent) {
        return new NodeImpl(name, value, parent);
    }

    void visitAll(Consumer<Node<V>> consumer);

    /**
     * Copy a transformed version of this node and it's children onto the specified parent.
     *
     * @param <T> The type of the value of the transformed copy
     * @param newParent The parent to copy a transformed version of this node and it's children to
     * @param nameConverter The converter which will be applied to produce a new name for each node in this tree
     * @param valueConverter The converter which will be applied to produce a new value for each node in this tree
     * @return The transformed copy of this node
     * @see #transform(Node, Function)
     */
    <T> Node<T> transform(Node<T> newParent, Function<Node<V>, String> nameConverter, Function<Node<V>, T> valueConverter);

    default boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Copy this node and it's children onto the specified parent.
     *
     * The value of this node is not deep copied. To achieve deep copy use the transform method. Use that method's
     * value converter to create a deep copy of the node's value.
     *
     * @param parent The parent to copy this node and it's children to
     * @return The copy version of this node
     * @see #transform(Node, Function)
     */
    default Node<V> copyTo(Node<V> parent) {
        return transform(parent, node -> node.getValueOrDefault(null));
    }

    default <T> Node<T> transform(Function<Node<V>, T> valueConverter) {
        return transform(Node.of(getName()), valueConverter);
    }

    /**
     * Copy a transformed version of this node and it's children onto the specified parent.
     *
     * @param newParent The parent to copy a transformed version of this node and it's children to
     * @param valueConverter The converter which will be applied to produce a new value for each node in this tree
     * @param <T> The type of the value of the transformed copy
     * @return The transformed copy of this node
     * @see #transform(Node, Function, Function)
     */
    default <T> Node<T> transform(Node<T> newParent, Function<Node<V>, T> valueConverter) {
        return transform(newParent, node -> node.getName(), valueConverter);
    }

    default <T> Node<T> transform(Function<Node<V>, String> nameConverter, Function<Node<V>, T> valueConverter) {
        return transform(Node.of(getName()), nameConverter, valueConverter);
    }

    default boolean isRoot() {
        return getParentOrDefault(null) == null;
    }

    default boolean isLeaf() {
        return getChildren().isEmpty();
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
        Node<V> target = this;
        while(target.getParentOrDefault(null) != null) {
            target = target.getParentOrDefault(null);
        }
        return target;
    }
    
    default Optional<Node<V>> findFirstChild(V... path) {
        
        return this.findFirst(this, path);
    }
    
    Optional<Node<V>> findFirst(Node<V> offset, V... path);

    default Optional<Node<V>> findFirstChild() {
        return findFirstChild(node -> true);
    }

    default Optional<Node<V>> findFirstChild(Predicate<Node<V>> nodeTest) {
        
        return this.findFirst(this, nodeTest);
    }

    Optional<Node<V>> findFirst(Node<V> offset, Predicate<Node<V>> nodeTest);

    Node<V> getChild(int index);

    /**
     * @return An <b>un-modifiable</b> list view of this node's children
     */
    List<Node<V>> getChildren();

    String getName();

    default boolean hasNodeValue() {
        return getValueOrDefault(null) != null;
    }

    default Optional<V> getValueOptional() {
        return Optional.ofNullable(this.getValueOrDefault(null));
    }

    V getValueOrDefault(V outputIfNone);

    default Optional<Node<V>> getParentOptional() {
        return Optional.ofNullable(this.getParentOrDefault(null));
    }
    
    Node<V> getParentOrDefault(Node<V> outputIfNone);
}
