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

import java.util.*;
import java.util.function.*;

/**
 * @author Chinomso Bassey Ikwuagwu on Oct 13, 2017 3:36:07 PM
 */
final class NodeImpl<V> implements Node<V> {

    private final String name;
    
    private final V value;
    
    private final Node<V> parent;

    private final List<Node<V>> children;

    NodeImpl(String name, V value, Node<V> parent) {
        this.name = Objects.requireNonNull(name);
        this.value = value;
        this.parent = parent;
        this.children = new LinkedList<>();
        if(parent != null) {
            if(this.equals(parent)) {
                throw new IllegalArgumentException("A node may not be parent to itself");
            }
            if(this.parent instanceof NodeImpl) {
                ((NodeImpl)this.parent).addChild(NodeImpl.this);
            }else{
                throw new IllegalArgumentException();
            }
        }
    }

    boolean addChild(Node<V> child) {
        final Object ref = child.getParentOrDefault(null);
        if(Objects.equals(ref, this)) {
            if(!this.children.contains(child)) {
                return this.children.add(child);
            }else{
                return false;
            }
        }else{
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void visitAll(Predicate<Node<V>> filter, Consumer<Node<V>> consumer, int depth) {
        new DepthFirstNodeVisitor<>(filter, consumer, depth).accept(this);
    }

    @Override
    public Node<V> copyTo(Node<V> parent) {
        final Node<V> newNode = Node.of(name, value, parent);
        children.forEach(child -> child.copyTo(newNode));
        return newNode;
    }

    @Override
    public Optional<Node<V>> findFirst(Node<V> offset, V... path) {
        for (V valueToFind : path) {
            final Predicate<Node<V>> nodeTest = (node) -> Objects.equals(node.getValueOrDefault(null), valueToFind);
            final Optional<Node<V>> foundNode = this.findFirst(offset, nodeTest);
            if(foundNode.isPresent()) {
                offset = foundNode.get();
            }else{
                offset = null;
                break;
            }
        }
        return Optional.ofNullable(offset);
    }
    
    @Override
    public Optional<Node<V>> findFirst(Node<V> offset, Predicate<Node<V>> nodeTest) {

        Node<V> found = null;

        if(nodeTest.test(offset)) {
            found = offset;
        }else{

            final List<Node<V>> childNodes = offset.getChildren();

            for(Node<V> child : childNodes) {

                final Optional<Node<V>> foundInChild = findFirst(child, nodeTest);

                if(foundInChild.isPresent()) {

                    found = foundInChild.get();

                    break;
                }
            }
        }

        return Optional.ofNullable(found);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public V getValueOrDefault(V outputIfNone) {
        return value == null ? outputIfNone : value;
    }
    

    @Override
    public Node<V> getParentOrDefault(Node<V> outputIfNone) {
        return parent == null ? outputIfNone : parent;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Override
    public Node<V> getChild(int index) { return children.get(index); }

    /**
     * @return An <b>un-modifiable</b> list view of this node's children
     */
    @Override
    public List<Node<V>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 11 * hash + Objects.hashCode(this.name);
        hash = 11 * hash + Objects.hashCode(this.value);
        hash = 11 * hash + Objects.hashCode(this.parent);
// To avoid stackoverflow, use either parent or children, but not both.
//        hash = 11 * hash + Objects.hashCode(this.children);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeImpl<?> other = (NodeImpl<?>) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        if (size() != other.size()) {
            return false;
        }
        if (!Objects.equals(this.parent, other.parent)) {
            return false;
        }
// To avoid stackoverflow, use either parent or children, but not both.
//        if (!Objects.equals(this.children, other.children)) {
//            return false;
//        }
        return true;
    }

    @Override
    public String toString() {
        return NodeFormatter.indentedHeirarchy().format(this);
    }
}
