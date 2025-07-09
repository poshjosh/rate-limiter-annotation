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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Chinomso Bassey Ikwuagwu on Oct 16, 2017 9:21:53 PM
 */
final class DepthFirstVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(DepthFirstVisitor.class.getName());

    static <V> void visitAll(MutableNode<V> node, Consumer<Node<V>> consumer) {
        visitAll(node, currentNode -> true, consumer, Integer.MAX_VALUE);
    }

    static <V> void visitAll(MutableNode<V> node, Predicate<Node<V>> filter,
            Consumer<Node<V>> consumer, int depth) {

        if(LOG.isTraceEnabled()) {
            LOG.trace("Depth: {}, visiting: {}", depth, node);
        }

        if(filter.test(node)) {
            consumer.accept(node);
            if(LOG.isTraceEnabled()) {
                LOG.trace("Depth, {}, processed: {}", depth, node);
            }
        }

        if (depth <= 0) {
            return;
        }

        final List<MutableNode<V>> childNodeSet = node.getChildren();

        for(MutableNode<V> childNode : childNodeSet) {

            visitAll(childNode, filter, consumer, depth-1);
        }
    }

    private DepthFirstVisitor() {
    }
}
