package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

final class MatchUtil {
    private static final Logger LOG = LoggerFactory.getLogger(MatchUtil.class);
    private MatchUtil() { }

    static <K> boolean matchesRateLimiters(
            Node<RateContext<K>> node,
            K key,
            RateLimiterProvider rateLimiterProvider,
            BiConsumer<String, RateLimiter> visitor) {
        int matchCount = visitMatchingRateLimiters(node, key, rateLimiterProvider, visitor);
        return matchSucceeded(node, matchCount);
    }

    private static <K> int visitMatchingRateLimiters(
            Node<RateContext<K>> node,
            K key,
            RateLimiterProvider rateLimiterProvider,
            BiConsumer<String, RateLimiter> visitor) {
        final RateContext<K> context = node == null ? null : node.getValueOrDefault(null);
        if (context == null) {
            return -1;
        }
        final String mainMatch = match(node, key);
        if (context.hasSubConditions()) {
            final int count = context.getSubMatchers().size();
            int matchCount = 0;
            for(int i = 0; i < count; i++) {
                final String match = matchAt(node, key, i, mainMatch);
                if (Matcher.isMatch(match)) {
                    ++matchCount;
                    RateLimiter rateLimiter =
                            rateLimiterProvider.getRateLimiter(match, context.getRate(i));

                    visitor.accept(match, rateLimiter);

                    if (LOG.isTraceEnabled()) {
                        LOG.trace("[{}]{} = {}", i, match, rateLimiter);
                    }
                }
            }
            return matchCount;
        } else {
            if (Matcher.isMatch(mainMatch)) {

                // We use parent rates as fallback. (Applies only to main matcher).
                //
                // This is useful for matchers which cannot match a rate source's parent.
                //
                // When a class or method is used as match candidate,
                // We can write a matcher like RateSourceMatcher which matches the candidate
                // with the rate source of the matcher and possibly the rate source's parent.
                // This is because we can use reflection to get a class or method's "parent".
                //
                // On the other hand, when an arbitrary value is used as match candidate,
                // we have to rely on other means. This fallback to the parent here gives
                // matchers based on this config an avenue to match the rate source's parent.
                //
                final Rates rates = context.getRatesWithParentRatesAsFallback();
                final RateLimiter rateLimiter = rateLimiterProvider.getRateLimiter(mainMatch, rates);

                visitor.accept(mainMatch, rateLimiter);

                if (LOG.isTraceEnabled()) {
                    LOG.trace("{} = {}", mainMatch, rateLimiter);
                }
                return 1;
            } else {
                return 0;
            }
        }
    }

    private static <K> String match(Node<RateContext<K>> node, K toMatch) {
        final RateContext<K> context = node.requireValue();
        final Matcher<K> matcher = context.getMainMatcher();
        final String match = matcher.match(toMatch);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Match: {}, toMatch: {}, matcher: {}",
                    Matcher.isMatch(match), toMatch, matcher);
        }
        return match;
    }

    private static <K> String matchAt(Node<RateContext<K>> node, K toMatch, int i, String mainMatch) {

        final RateContext<K> context = node.requireValue();

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

    private static <K> boolean matchSucceeded(Node<RateContext<K>> node, int matchCount) {
        RateContext<K> context = node.getValueOrDefault(null);
        if (context == null) {
            return false;
        }
        if (context.hasSubConditions()) {
            return matchCount >= context.getSubMatchers().size();
        } else {
            return matchCount >= 1;
        }
    }
}
