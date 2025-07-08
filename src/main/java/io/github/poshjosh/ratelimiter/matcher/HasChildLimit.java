package io.github.poshjosh.ratelimiter.matcher;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;

import java.util.function.Predicate;

final class HasChildLimit implements Predicate<Node<RateConfig>> {

    @Override
    public boolean test(Node<RateConfig> node) {
        return node.requireValue().getRates().isSet() || anyChildHasLimit(node);
    }

    private boolean anyChildHasLimit(Node<RateConfig> node) {
        final int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            Node<RateConfig> child = node.getChild(i);
            if (child.hasValue() && test(child)) {
                return true;
            }
        }
        return false;
    }
}
