package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.node.Node;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

class RateLimiterComposite<K> extends AbstractRateLimiterComposite<K> implements RateLimiter {
    private final Node<RateContext<K>> rootNode;

    RateLimiterComposite (K key,
            Node<RateContext<K>> rootNode,
            RateLimiterProvider rateLimiterProvider) {
        super(key, rateLimiterProvider);
        this.rootNode = Objects.requireNonNull(rootNode);
    }

    @Override
    protected void visitNodes(BiConsumer<String, RateLimiter> visitor) {
        AtomicBoolean matchFound = new AtomicBoolean(false);
        AtomicBoolean firstLeafAfterMatch = new AtomicBoolean(false);
        rootNode.visitAll(
                node -> !firstLeafAfterMatch.get(),
                node -> {
                    // We still need to traverse the entire current branch, even if we find a match
                    // However, we stop at the current branch, if we find a match in it.
                    if (matchesRateLimiters(node, visitor)) {
                        matchFound.set(true);
                    }
                    if (matchFound.get() && node.isLeaf()) {
                        firstLeafAfterMatch.set(true);
                    }
                });
    }
}
