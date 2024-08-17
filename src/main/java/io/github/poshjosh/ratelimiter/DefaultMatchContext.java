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
    private final List<Matcher<INPUT>> limitMatchers;

    DefaultMatchContext(RateConfig rateConfig,
            Matcher<INPUT> mainMatcher, List<Matcher<INPUT>> limitMatchers) {
        this.rateConfig = Objects.requireNonNull(rateConfig);
        this.mainMatcher = Objects.requireNonNull(mainMatcher);
        this.limitMatchers = Objects.requireNonNull(limitMatchers);
    }

    @Override public boolean matches(INPUT key, MatchVisitor<?> matchVisitor) {
        final int matchCount = visitMatching(key, matchVisitor);
        return isMatchSuccessful(matchCount);
    }

    private int visitMatching(INPUT key, MatchVisitor<?> matchVisitor) {
        final String mainMatch = match(key);
        if (hasSubConditions()) {
            final int count = getLimitMatchers().size();
            int matchCount = 0;
            for(int i = 0; i < count; i++) {
                final String match = matchAt(key, i, mainMatch);
                if (Matcher.isMatch(match)) {
                    ++matchCount;

                    final Rate rate = rateAt(i);

                    matchVisitor.visit(match, rate);

                    if (LOG.isTraceEnabled()) {
                        LOG.trace("[{}]{} = {}", i, match, rate);
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
                    LOG.trace("{} = {}", mainMatch, rates);
                }
                return 1;
            } else {
                return 0;
            }
        }
    }

    private String match(INPUT key) {
        final Matcher<INPUT> matcher = getMainMatcher();
        final String match = matcher.match(key);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Match: {}, toMatch: {}, matcher: {}",
                    Matcher.isMatch(match), key, matcher);
        }
        return match;
    }

    private String matchAt(INPUT key, int i, String mainMatch) {

        final Matcher<INPUT> matcher = getLimitMatchers().get(i);

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
            return matchCount >= getLimitMatchers().size();
        } else {
            return matchCount >= 1;
        }
    }

    @Override public boolean hasMatcher() {
        return !Matchers.matchNone().equals(mainMatcher) ||
                limitMatchers.stream().anyMatch(matcher -> !Matchers.matchNone().equals(matcher));
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
        return rateConfig.getRates().getSubLimits().get(index);
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

    @Override public List<Matcher<INPUT>> getLimitMatchers() {
        return limitMatchers;
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
                && limitMatchers.equals(that.limitMatchers);
    }

    public int hashCode() {
        return Objects.hash(rateConfig, mainMatcher, limitMatchers);
    }

    public String toString() {
        return "DefaultMatchContext{config=" + rateConfig +
                ", mainMatcher=" + mainMatcher + ", limitMatchers=" + limitMatchers + '}';
    }
}
