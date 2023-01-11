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
import java.util.Objects;

/**
 * @author Chinomso Bassey Ikwuagwu on Oct 19, 2017 5:04:09 PM
 */
public abstract class NodeFormatter {

    private static class Indented extends NodeFormatter{

        private final String indent;

        private Indented(String indent) {
            this.indent = Objects.requireNonNull(indent);
        }

        @Override
        public <V> StringBuilder appendTo(Node<V> node, StringBuilder appendTo) {
            if(node == null) {
                return appendTo.append((Object)null);
            }
            final int nodeLevel = node.getLevel();
            for(int i=nodeLevel; i>=0; i--) {
                appendTo.append(indent);
            }
            return appendTo.append(node.getName()).append('=').append(node.getValueOrDefault(null));
        }
    }

    private static class IndentedHeirarchy extends NodeFormatter{

        private final String indent;

        private final int maxLevelsToPrint;

        private IndentedHeirarchy(String indent, int maxLevelsToPrint) {
            this.indent = Objects.requireNonNull(indent);
            this.maxLevelsToPrint = maxLevelsToPrint;
        }

        @Override
        public <V> StringBuilder appendTo(Node<V> node, StringBuilder appendTo) {
            return appendTo(node, appendTo, maxLevelsToPrint);
        }

        private <V> StringBuilder appendTo(Node<V> node, StringBuilder appendTo, int levelsRemaining) {
            this.doAppendTo(node, appendTo);
            if(levelsRemaining > 0) {
                final List<Node<V>> children = node.getChildren();
                for(Node<V> child : children) {
                    this.appendTo(child, appendTo, levelsRemaining - 1);
                }
            }
            return appendTo;
        }

        private <V> void doAppendTo(Node<V> node, StringBuilder appendTo) {
            if(node == null) {
                appendTo.append(node);
                return;
            }
            appendTo.append('\n');
            final int nodeLevel = node.getLevel();
            for(int i=nodeLevel; i>=0; i--) {
                appendTo.append(indent);
            }
            appendTo.append(node.getName()).append('=').append(node.getValueOrDefault(null));
        }
    }

    public static NodeFormatter indented() {
        return indented("  ");
    }

    public static NodeFormatter indented(String indent) {
        return new NodeFormatter.Indented(indent);
    }

    public static NodeFormatter indentedHeirarchy() {
        return indentedHeirarchy("  ");
    }

    public static NodeFormatter indentedHeirarchy(String indent) {
        return indentedHeirarchy(indent, Integer.MAX_VALUE);
    }

    public static NodeFormatter indentedHeirarchy(String indent, int maxLevelsToPrint) {
        return new NodeFormatter.IndentedHeirarchy(indent, maxLevelsToPrint);
    }

    protected NodeFormatter() { }

    public abstract <V> StringBuilder appendTo(Node<V> node, StringBuilder appendTo);

    public <V> String format(Node<V> node) {
        return appendTo(node, new StringBuilder()).toString();
    }
}
