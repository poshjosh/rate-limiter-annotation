package io.github.poshjosh.ratelimiter.matcher;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;

import java.util.function.Predicate;

final class HasParentLimit implements Predicate<Node<RateConfig>> {

    @Override
    public boolean test(Node<RateConfig> node) {
        return node.requireValue().getRates().isSet() || anyParentHasLimit(node);
    }

    private boolean anyParentHasLimit(Node<RateConfig> node) {
        Node<RateConfig> parentNode = node.getParentOrDefault(null);
        if (parentNode == null) {
            return false;
        }
        return parentNode.hasValue() && test(parentNode);
    }
}
