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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

/**
 * @author Chinomso Bassey Ikwuagwu on Oct 13, 2017 3:36:07 PM
 */
final class NodeImpl<V> implements MutableNode<V> {

    private final String name;
    
    private final V value;
    
    private final Node<V> parent;

    NodeImpl(String name, V value, Node<V> parent) {
        this.name = Objects.requireNonNull(name);
        this.value = value;
        this.parent = parent;
        if(parent != null) {
            if(this.equals(parent)) {
                throw new IllegalArgumentException("A node may not be parent to itself");
            }
            if(this.parent instanceof MutableNode) {
                ((MutableNode<V>)this.parent).addChild(NodeImpl.this);
            }else{
                throw new IllegalArgumentException(
                        "Parent node must be an instance of MutableNode");
            }
        }
    }

    private List<Node<V>> _children = null;
    @Override
    public boolean addChild(Node<V> child) {
        final Node<V> parentNode = child.getParentOrDefault(null);
        if (!Objects.equals(parentNode, this)) {
            throw new UnsupportedOperationException();
        }
        if (_children == null) {
            _children = new ArrayList<>();
            return _children.add(child);
        }

        if(!_children.contains(child)) {
            return _children.add(child);
        }

        return false;
    }

    public Node<V> removeChild(String name) {
        if (_children == null) {
            return null;
        }
        Node<V> removed = null;
        final Iterator<Node<V>> iter = _children.iterator();
        while(iter.hasNext()) {
            final Node<V> child = iter.next();
            if(Objects.equals(child.getName(), name)) {
                iter.remove();
                removed = child;
                break;
            }
        }
        return removed;
    }

    @Override
    public boolean anyChildMatch(Predicate<Node<V>> test) {
        return children().stream().anyMatch(child -> child.anyMatch(test));
    }

    @Override
    public int size() {
        if (isLeaf()) {
            return 1;
        }
        int size = 1;
        for (Node<V> child : children()) {
            size += child.size();
        }
        return size;
    }

    private List<Node<V>> children() {
        return _children == null ? Collections.emptyList() : _children;
    }

    @Override
    public void visitAll(Predicate<Node<V>> filter, Consumer<Node<V>> consumer, int depth) {
        DepthFirstVisitor.visitAll(this, filter, consumer, depth);
    }

    @Override
    public Node<V> copyTo(Node<V> parent) {
        final Node<V> newNode = Nodes.of(name, value, parent);
        children().forEach(child -> child.copyTo(newNode));
        return newNode;
    }

    public Node<V> findFirstOrDefault(
            Node<V> offset, Predicate<Node<V>> nodeTest, Node<V> resultIfNone) {
        Node<V> found = null;
        if(nodeTest.test(offset)) {
            found = offset;
        } else {
            // offset.getChildren().stream() returns a copy which is less performant
            final int childCount = offset.getChildCount();
            for(int i = 0; i < childCount; i++) {
                Node<V> child = offset.getChild(i);
                found = findFirstOrDefault(child, nodeTest, null);
                if(found != null) {
                    break;
                }
            }
        }
        return found == null ? resultIfNone : found;
    }

    private Node<V>[] leafs;
    /**
     * Get leaf child nodes (excluding root node, or {@link Nodes#EMPTY}).
     * @return leaf child nodes (excluding root node, or {@link Nodes#EMPTY})..
     * @see #isLeaf()
     * @see #isRoot()
     * @see #isEmptyNode()
     */
    public Node<V>[] collectLeafs() {
        Set<Node<V>> leafNodes = new LinkedHashSet<>();
        Predicate<Node<V>> test = n -> n.isLeaf() && !n.isRoot() && !n.isEmptyNode();
        visitAll(test, leafNodes::add);
        leafs = leafNodes.toArray(new Node[0]);
        return leafs;
    }

    /**
     * Get leaf child nodes, earlier collected via method {@link #collectLeafs()}, or fail.
     * @return leaf child nodes, earlier collected via method {@link #collectLeafs()}, or fail.
     * @see #collectLeafs()
     * @throws UnsupportedOperationException if {@link #collectLeafs()} was not earlier called.
     */
    @Override
    public Node<V>[] getCollectedLeafs() throws UnsupportedOperationException {
        if (leafs == null) {
            throw new UnsupportedOperationException(
                    "#collectLeafs() must have been called, before calling getCollectedLeafs()");
        }
        return leafs;
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
        return !children().isEmpty();
    }

    @Override
    public Node<V> getChild(int index) { return children().get(index); }

    @Override
    public int getChildCount() {
        return children().size();
    }

    /**
     * @return An unmodifiable list of this node's children
     */
    @Override
    public List<Node<V>> getChildren() {
        return Collections.unmodifiableList(children());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 11 * hash + Objects.hashCode(this.name);
        hash = 11 * hash + Objects.hashCode(this.value);
        hash = 11 * hash + Objects.hashCode(this.parent);
// To avoid stackoverflow, use either parent or children, but not both.
//        hash = 11 * hash + Objects.hashCode(this.children());
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
        if (!Objects.equals(this.parent, other.parent)) {
            return false;
        }
// To avoid stackoverflow, use either parent or children, but not both.
//        if (!Objects.equals(this.children(), other.children())) {
//            return false;
//        }
        return true;
    }

    @Override
    public String toString() {
        return NodeFormatter.indentedHeirarchy().format(this);
    }
}
