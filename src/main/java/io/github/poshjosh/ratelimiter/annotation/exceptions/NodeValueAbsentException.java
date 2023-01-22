package io.github.poshjosh.ratelimiter.annotation.exceptions;

import io.github.poshjosh.ratelimiter.node.Node;

public final class NodeValueAbsentException extends AnnotationProcessingException{
    public NodeValueAbsentException() {
        super("Value is required for node");
    }
    public NodeValueAbsentException(Node<?> node) {
        super("Value is required for: " + node.getName());
    }
}
