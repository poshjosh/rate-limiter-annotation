package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

final class DefaultMatchContext<INPUT> implements MatchContext<INPUT> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMatchContext.class);

    private final RateConfig rateConfig;

    /**
     * The matcher to apply before applying sub matchers.
     */
    private final Matcher<INPUT> mainMatcher;

    /**
     * Matchers for rate conditions specific to each rate.
     */
    private final List<Matcher<INPUT>> subMatchers;

    DefaultMatchContext(RateConfig rateConfig,
            Matcher<INPUT> mainMatcher, List<Matcher<INPUT>> subMatchers) {
        this.rateConfig = Objects.requireNonNull(rateConfig);
        this.mainMatcher = Objects.requireNonNull(mainMatcher);
        this.subMatchers = Objects.requireNonNull(subMatchers);
    }

    @Override public boolean matches(INPUT key, MatchVisitor<?> matchVisitor) {
        final int matchCount = visitMatching(key, matchVisitor);
        return isMatchSuccessful(matchCount);
    }

    private int visitMatching(INPUT key, MatchVisitor<?> matchVisitor) {
        final String mainMatch = matchMain(key);
        if (hasSubConditions()) {
            final int count = getSubMatchers().size();
            int matchCount = 0;
            for(int i = 0; i < count; i++) {
                // If there are sub conditions, then the main match is used in conjunction with each
                final String match = matchAt(key, i, mainMatch);
                if (Matcher.isMatch(match)) {
                    ++matchCount;

                    final Rate rate = rateAt(i);

                    matchVisitor.visit(match, rate);

                    if (LOG.isTraceEnabled()) {
                        LOG.trace("At [{}], matched '{}' to {}", i, match, rate);
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
                final Rates rates = ratesOrParentRates();
                matchVisitor.visit(mainMatch, rates);

                if (LOG.isTraceEnabled()) {
                    LOG.trace("Matched '{}' to {}", mainMatch, rates);
                }
                return 1;
            } else {
                return 0;
            }
        }
    }

    private String matchMain(INPUT key) {
        final Matcher<INPUT> matcher = getMainMatcher();
        final String match = matcher.match(key);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Match: {}, toMatch: {}, matcher: {}",
                    Matcher.isMatch(match), key, matcher);
        }
        return match;
    }

    private String matchAt(INPUT key, int i, String mainMatch) {

        final Matcher<INPUT> matcher = getSubMatchers().get(i);

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

    private boolean isMatchSuccessful(int matchCount) {
        if (hasSubConditions()) {
            return matchCount >= getSubMatchers().size();
        } else {
            return matchCount >= 1;
        }
    }

    @Override public boolean hasMatcher() {
        return !Matchers.matchNone().equals(mainMatcher) ||
                subMatchers.stream().anyMatch(matcher -> !Matchers.matchNone().equals(matcher));
    }

    @Override public String getId() {
        return rateConfig.getId();
    }

    @Override public boolean hasSubConditions() {
        return rateConfig.getRates().hasSubConditions();
    }

    @Override public RateSource getSource() { return rateConfig.getSource(); }

    @Override public Rate getRate(int index) {
        return Rate.of(rateAt(index));
    }
    
    private Rate rateAt(int index) {
        return rateConfig.getRates().getRates().get(index);
    }

    @Override public Rates getRates() {
        return rateConfig.getRates().copy();
    }

    @Override public Rates getRatesOrParentRates() {
        return ratesOrParentRates().copy();
    }
    
    private Rates ratesOrParentRates() {
        return rateConfig.getRatesOrParentRates();
    }

    @Override public Matcher<INPUT> getMainMatcher() { return mainMatcher; }

    @Override public List<Matcher<INPUT>> getSubMatchers() {
        return subMatchers;
    }

    @Override public RateConfig getRateConfig() {
        return RateConfig.of(rateConfig.getSource(), rateConfig.getRates(), rateConfig.getParent());
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DefaultMatchContext<?> that = (DefaultMatchContext<?>) o;
        return rateConfig.equals(that.rateConfig) && mainMatcher.equals(that.mainMatcher)
                && subMatchers.equals(that.subMatchers);
    }

    public int hashCode() {
        return Objects.hash(rateConfig, mainMatcher, subMatchers);
    }

    public String toString() {
        return "DefaultMatchContext{config=" + rateConfig +
                ", mainMatcher=" + mainMatcher + ", subMatchers=" + subMatchers + '}';
    }
}
