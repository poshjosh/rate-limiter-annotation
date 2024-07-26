package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.node.Node;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * In bottom-up traversal, if we traverse all the branches in the tree,
 * we will encounter some nodes twice. This happens because 2 or more
 * leaf nodes may share the same parent.
 * Fortunately we don't need to traverse all the branches in the tree.
 * We stop after the current branch once we find a match.
 * @param <K>
 */
final class RateLimiterCompositeBottomUp<K>
        extends AbstractRateLimiterComposite<K>
        implements RateLimiter {
    private final Node<RateContext<K>>[] leafNodes;

    RateLimiterCompositeBottomUp (K key,
            Node<RateContext<K>>[] leafNodes,
            RateLimiterProvider rateLimiterProvider) {
        super(key, rateLimiterProvider);
        this.leafNodes = Objects.requireNonNull(leafNodes);
    }

    protected void visitNodes(BiConsumer<String, RateLimiter> visitor) {
        for (Node<RateContext<K>> node : leafNodes) {
            boolean atLeastOneNodeInBranchMatched = false;
            do {
                // We need to traverse the entire current branch, even if we find a match.
                // However, we stop at the current branch, if we find a match in it.
                if (matchesRateLimiters(node, visitor)) {
                    atLeastOneNodeInBranchMatched = true;
                }
                node = node.getParentOrDefault(null);
            } while(node != null);
            // If at least one node in the last branch matches, we skip
            // the subsequent branches
            if (atLeastOneNodeInBranchMatched) {
                break;
            }
        }
    }
}
