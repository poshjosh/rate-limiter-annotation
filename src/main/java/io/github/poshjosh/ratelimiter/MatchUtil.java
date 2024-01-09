package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.LimiterConfig;
import io.github.poshjosh.ratelimiter.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.GenericDeclaration;
import java.util.Objects;

final class MatchUtil {
    private static final Logger LOG = LoggerFactory.getLogger(MatchUtil.class);
    private MatchUtil() { }

    static <K> String match(Node<LimiterConfig<K>> node, K toMatch) {
        final LimiterConfig<K> config = requireLimiterConfig(node);
        final Matcher<K> matcher = config.getMatcher();
        final String match = matcher.match(toMatch);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Match: {}, toMatch: {}, matcher: {}",
                    Matcher.isMatch(match), toMatch, matcher);
        }
        return resolveMatch(match, node);

    }

    static <K> String matchAt(Node<LimiterConfig<K>> node, K toMatch, int i, String groupMatchResult) {

        final LimiterConfig<K> config = requireLimiterConfig(node);

        final Matcher<K> matcher = config.getMatchers().get(i);

        final String match = matcher.match(toMatch);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Match: {}, node[{}] toMatch: {}, matcher: {}",
                    Matcher.isMatch(match), i, toMatch, matcher);
        }

        if (!Matcher.isMatch(match)) {
            return Matcher.NO_MATCH;
        }

        return Matcher.composeResults(groupMatchResult, match);
    }

    private static <K> String resolveMatch(String match, Node<LimiterConfig<K>> node) {
        final LimiterConfig<K> config = requireLimiterConfig(node);
        if (config.getSource().isGroupType()) {
            return node.getName();
        }
        if (!Matcher.isMatch(match) && !isGenericDeclarationSource(config) && !node.isLeaf()) {
            return node.getName();
        }
        return match;
    }

    private static boolean isGenericDeclarationSource(LimiterConfig<?> config) {
        return config.getSource().getSource() instanceof GenericDeclaration;
    }

    private static <K> LimiterConfig<K> requireLimiterConfig(Node<LimiterConfig<K>> node) {
        return Objects.requireNonNull(node.getValueOrDefault(null));
    }
}
