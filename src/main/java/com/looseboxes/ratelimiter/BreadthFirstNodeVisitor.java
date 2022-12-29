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
package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Chinomso Bassey Ikwuagwu on Oct 16, 2017 9:21:53 PM
 */
final class BreadthFirstNodeVisitor<T> implements Consumer<Node<T>>{

    private static final Logger LOG = LoggerFactory.getLogger(BreadthFirstNodeVisitor.class.getName());

    private final Predicate<Node<T>> filter;
    
    private final Consumer<Node<T>> consumer;
    
    private final int depth;

    BreadthFirstNodeVisitor(Consumer<Node<T>> consumer) {
        this(node -> true, consumer);
    }

    BreadthFirstNodeVisitor(Predicate<Node<T>> filter, Consumer<Node<T>> consumer) {
        this(filter, consumer, Integer.MAX_VALUE);
    }

    BreadthFirstNodeVisitor(Predicate<Node<T>> filter, Consumer<Node<T>> consumer, int depth) {
        this.filter = Objects.requireNonNull(filter);
        this.consumer = Objects.requireNonNull(consumer);
        this.depth = depth;
    }
    
    @Override
    public void accept(Node<T> node) {
        
        this.visit(node, this.depth);
    }
    
    public void visit(Node<T> node, int depth) {

        if(LOG.isTraceEnabled()) {
            LOG.trace("Visiting: {}", node);
        }

        this.visit(filter, consumer, node);
        
        if(depth > 0) {
        
            final List<Node<T>> childNodeSet = node.getChildren();

            for(Node<T> childNode : childNodeSet) {

                this.visit(childNode, depth-1);
            }
        }
    }
    
    private void visit(Predicate<Node<T>> test, Consumer<Node<T>> action, Node<T> node) {

        if(test.test(node)) {

            action.accept(node);

            if(LOG.isTraceEnabled()) {
                LOG.trace("Processed node: {}", node);
            }
        }
    }
}
