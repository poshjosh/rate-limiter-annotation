package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Matchers;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

abstract class AbstractRateLimiterComposite<K> implements RateLimiter {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRateLimiterComposite.class);
    private final K key;
    private final RateLimiterProvider rateLimiterProvider;

    protected AbstractRateLimiterComposite(K key, RateLimiterProvider rateLimiterProvider) {
        this.key = Objects.requireNonNull(key);
        this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
    }

    protected abstract void visitNodes(BiConsumer<String, RateLimiter> visitor);

    @Override
    public double acquire(int permits) {
        PermitAcquiringVisitor visitor = new PermitAcquiringVisitor(permits);
        visitNodes(visitor);
        return visitor.getTotalTimeSpent();
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        PermitAttemptingVisitor visitor = new PermitAttemptingVisitor(permits, timeout, unit);
        visitNodes(visitor);
        return visitor.isNoLimitExceeded();
    }

    @Override
    public Bandwidth getBandwidth() {
        List<Bandwidth> bandwidths = new ArrayList<>();
        BiConsumer<String, RateLimiter> visitor = (match, rateLimiter) ->
                bandwidths.add(rateLimiter.getBandwidth());
        visitNodes(visitor);
        // For multiple Bandwidths conjugated with Operator.OR, the composed Bandwidth
        // succeeds only when all Bandwidths succeed. This is the case here.
        return Bandwidths.of(Operator.OR, bandwidths.toArray(new Bandwidth[0]));
    }

    protected boolean matchesRateLimiters(
            Node<RateContext<K>> node,
            BiConsumer<String, RateLimiter> visitor) {
        final RateContext<K> rateContext = node == null ? null : node.getValueOrDefault(null);
        int matchCount = visitMatchingRateLimiters(rateContext, visitor);
        return matchSucceeded(rateContext, matchCount);
    }

    private int visitMatchingRateLimiters(
            RateContext<K> rateContext,
            BiConsumer<String, RateLimiter> visitor) {
        if (rateContext == null) {
            return -1;
        }
        final String mainMatch = match(rateContext);
        if (rateContext.hasSubConditions()) {
            final int count = rateContext.getLimitMatchers().size();
            int matchCount = 0;
            for(int i = 0; i < count; i++) {
                final String match = matchAt(rateContext, i, mainMatch);
                if (Matcher.isMatch(match)) {
                    ++matchCount;
                    RateLimiter rateLimiter =
                            rateLimiterProvider.getRateLimiter(match, rateContext.getRate(i));

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
                final Rates rates = rateContext.getRatesWithParentRatesAsFallback();
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

    private String match(RateContext<K> rateContext) {
        final Matcher<K> matcher = rateContext.getMainMatcher();
        final String match = matcher.match(key);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Match: {}, toMatch: {}, matcher: {}",
                    Matcher.isMatch(match), key, matcher);
        }
        return match;
    }

    private String matchAt(RateContext<K> rateContext, int i, String mainMatch) {

        final Matcher<K> matcher = rateContext.getLimitMatchers().get(i);

        final String match = matcher.match(key);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Match: {}, node[{}] toMatch: {}, matcher: {}",
                    Matcher.isMatch(match), i, key, matcher);
        }

        if (!Matcher.isMatch(match)) {
            return Matchers.NO_MATCH;
        }

        return Matcher.composeResults(mainMatch, match);
    }

    private boolean matchSucceeded(RateContext<K> rateContext, int matchCount) {
        if (rateContext == null) {
            return false;
        }
        if (rateContext.hasSubConditions()) {
            return matchCount >= rateContext.getLimitMatchers().size();
        } else {
            return matchCount >= 1;
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(256)
                .append(this.getClass().getSimpleName())
                .append('@')
                .append(Integer.toHexString(hashCode()))
                .append('{');
        final int lengthBeforeVisit = builder.length();
        BiConsumer<String, RateLimiter> visitor = (match, rateLimiter) ->
                builder.append("\n\tmatch=").append(match).append(", limiter=").append(rateLimiter);
        visitNodes(visitor);
        if (builder.length() > lengthBeforeVisit) {
            builder.append('\n');
        }
        builder.append('}');
        return builder.toString();
    }
}
