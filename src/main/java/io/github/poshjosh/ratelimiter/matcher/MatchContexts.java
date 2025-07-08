package io.github.poshjosh.ratelimiter.matcher;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.node.MutableNode;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.MatcherProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public final class MatchContexts {

    private static final Logger LOG = LoggerFactory.getLogger(MatchContexts.class);

    // Bottom-up traversal consumes about 7x less memory, as of the last tests.
    private static final boolean IS_BOTTOM_UP_TRAVERSAL = true;
    private static final Predicate<Node<RateConfig>> HAS_LIMIT =
            IS_BOTTOM_UP_TRAVERSAL ? new HasParentLimit() : new HasChildLimit();

    public static <K> MatchContext<K> of(
            MatcherProvider<K> matcherProvider,
            Node<RateConfig> node) {
        RateConfig rateConfig = node.getValueOrDefault(null);
        LOG.trace("{}", rateConfig);
        if (rateConfig == null) {
            return null;
        }
        Matcher<K> mainMatcher;
        List<Matcher<K>> limitMatchers;
        if(!HAS_LIMIT.test(node) && !rateConfig.shouldDelegateToParent()) {
            LOG.debug("No limits specified for group, so no matcher will be created for: {}",
                    node.getName());
            mainMatcher = Matchers.matchNone();
            limitMatchers = Collections.emptyList();
        } else {
            mainMatcher = matcherProvider.createMainMatcher(rateConfig);
            limitMatchers = matcherProvider.createSubMatchers(rateConfig);
            // Tag:Rule:number-of-matchers-must-equal-number-of-rates
            if (limitMatchers.size() != rateConfig.getRates().subRateSize()) {
                throw new IllegalStateException(
                        String.format("Number of Matchers: %s is not equal to number of rates: %s",
                                limitMatchers.size(), rateConfig.getRates().subRateSize()));
            }
        }
        final MatchContext<K> matchContext =
                new DefaultMatchContext<>(rateConfig, mainMatcher, limitMatchers);
        LOG.trace("{}", matchContext);
        return matchContext;
    }

    public static <K> void visitNodes(
            Node<MatchContext<K>> rootNode,
            K toMatch,
            MatchVisitor<?> matchVisitor) {
        if (IS_BOTTOM_UP_TRAVERSAL) {
            visitNodesBottomUp(((MutableNode)rootNode).getCollectedLeafs(), toMatch, matchVisitor);
        } else {
            visitNodesTopDown(rootNode, toMatch, matchVisitor);
        }
    }

    private static <K> void visitNodesTopDown(
            Node<MatchContext<K>> rootNode,
            K toMatch,
            MatchVisitor<?> matchVisitor) {
        AtomicBoolean matchFound = new AtomicBoolean(false);
        AtomicBoolean firstLeafAfterMatch = new AtomicBoolean(false);
        rootNode.visitAll(
                node -> !firstLeafAfterMatch.get(),
                node -> {
                    // We still need to traverse the entire current branch, even if we find a match
                    // However, we stop at the current branch, if we find a match in it.
                    if (matches(toMatch, node, matchVisitor)) {
                        matchFound.set(true);
                    }
                    if (matchFound.get() && node.isLeaf()) {
                        firstLeafAfterMatch.set(true);
                    }
                });
    }

    private static <K> void visitNodesBottomUp(
            Node<MatchContext<K>>[] leafNodes,
            K toMatch,
            MatchVisitor<?> matchVisitor) {
        for (Node<MatchContext<K>> node : leafNodes) {
            boolean atLeastOneNodeInBranchMatched = false;
            do {
                // We need to traverse the entire current branch, even if we find a match.
                // However, we stop at the current branch, if we find a match in it.
                if (matches(toMatch, node, matchVisitor)) {
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

    private static <K> boolean matches(
            K toMatch,
            Node<MatchContext<K>> node,
            MatchVisitor<?> matchVisitor) {
        final MatchContext<K> matchContext = node == null ? null : node.getValueOrDefault(null);
        if (matchContext == null) {
            return false;
        }
        return matchContext.matches(toMatch, matchVisitor);
    }

    private MatchContexts() { }
}
