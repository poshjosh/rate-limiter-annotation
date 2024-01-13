package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.LimiterContext;
import io.github.poshjosh.ratelimiter.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.GenericDeclaration;

final class MatchUtil {
    private static final Logger LOG = LoggerFactory.getLogger(MatchUtil.class);
    private MatchUtil() { }

    static <K> String match(Node<LimiterContext<K>> node, K toMatch) {
        final LimiterContext<K> context = node.requireValue();
        final Matcher<K> matcher = context.getMainMatcher();
        final String match = matcher.match(toMatch);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Match: {}, toMatch: {}, matcher: {}",
                    Matcher.isMatch(match), toMatch, matcher);
        }
        return resolveMatch(match, node);

    }

    static <K> String matchAt(Node<LimiterContext<K>> node, K toMatch, int i, String mainMatch) {

        final LimiterContext<K> context = node.requireValue();

        final Matcher<K> matcher = context.getSubMatchers().get(i);

        final String match = matcher.match(toMatch);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Match: {}, node[{}] toMatch: {}, matcher: {}",
                    Matcher.isMatch(match), i, toMatch, matcher);
        }

        if (!Matcher.isMatch(match)) {
            return Matcher.NO_MATCH;
        }

        return Matcher.composeResults(mainMatch, match);
    }

    private static <K> String resolveMatch(String match, Node<LimiterContext<K>> node) {
        final LimiterContext<K> context = node.requireValue();
        if (context.isGroupSource()) {
            return node.getName();
        }
        if (!Matcher.isMatch(match) && !context.isGenericDeclarationSource() && !node.isLeaf()) {
            return node.getName();
        }
        return match;
    }
}
