package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.*;
import io.github.poshjosh.ratelimiter.annotation.RateSource;
import io.github.poshjosh.ratelimiter.annotation.exceptions.NodeValueAbsentException;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class LimiterConfig<R> {

    private static final Logger LOG = LoggerFactory.getLogger(LimiterConfig.class);

    public static <K> LimiterConfig<K> of(
            RateToBandwidthConverter converter,
            MatcherProvider<K> matcherProvider,
            Ticker ticker,
            Node<RateConfig> node) {
        RateConfig rateConfig = node.getValueOrDefault(null);
        if (rateConfig == null) {
            return null;
        }
        Rates rates = rateConfig.getRates();
        Bandwidth[] bandwidths = converter.convert(node.getName(), rates, ticker.elapsedMicros());
        Matcher<K> matcher;
        List<Matcher<K>> matchers;
        if(!hasLimitsInTree(node)) {
            LOG.debug("No limits specified for group, so no matcher will be created for: {}",
                    node.getName());
            matcher = Matcher.matchNone();
            matchers = Collections.emptyList();
        } else {
            matcher = matcherProvider.createMatcher(rateConfig);
            matchers = matcherProvider.createMatchers(rateConfig);
        }
        return of(rateConfig.getSource(), rates, bandwidths, matcher, matchers, ticker);
    }
    private static boolean hasLimitsInTree(Node<RateConfig> node) {
        return hasLimits(node) || parentHasLimits(node);
    }
    private static boolean parentHasLimits(Node<RateConfig> node) {
        return node.getParentOptional()
                .filter(parent -> parent.hasValue() && hasLimitsInTree(parent))
                .isPresent();
    }
    private static boolean hasLimits(Node<RateConfig> node) {
        return node.getValueOptional().orElseThrow(() -> new NodeValueAbsentException(node))
                .getRates().hasLimits();
    }

    public static <R> LimiterConfig<R> of(RateSource source, Rates rates, Bandwidth[] bandwidths,
            Matcher<R> matcher, List<Matcher<R>> matchers, Ticker ticker) {
        return new LimiterConfig<>(source, rates, bandwidths, matcher, matchers, ticker);
    }

    private final RateSource source;

    private final Rates rates;

    private final Bandwidth [] bandwidths;

    /**
     * The matcher to apply before applying individual matchers. This is usually
     * the path pattern matcher (and then) any matchers for general rate conditions.
     */
    private final Matcher<R> matcher;

    /**
     * Matchers for rate conditions specific to each rate.
     */
    private final List<Matcher<R>> matchers;

    private final Ticker ticker;

    private LimiterConfig(RateSource source, Rates rates, Bandwidth[] bandwidths,
            Matcher<R> matcher, List<Matcher<R>> matchers, Ticker ticker) {
        this.source = Objects.requireNonNull(source);
        this.rates = Rates.of(rates);
        this.bandwidths = Arrays.copyOf(bandwidths, bandwidths.length);
        this.matcher = Objects.requireNonNull(matcher);
        this.matchers = Collections.unmodifiableList(new ArrayList<>(matchers));
        this.ticker = Objects.requireNonNull(ticker);
    }

    public String getId() {
        return source.getId();
    }

    public boolean hasLimits() {
        return rates.hasLimits();
    }

    public boolean hasChildConditions() {
        return rates.hasChildConditions();
    }

    public RateSource getSource() { return source; }

    public Rates getRates() {
        return Rates.of(rates);
    }

    public Bandwidth [] getBandwidths() { return Arrays.copyOf(bandwidths, bandwidths.length); }

    public Matcher<R> getMatcher() { return matcher; }

    public List<Matcher<R>> getMatchers() {
        return matchers;
    }

    public Ticker getSleepingTicker() { return ticker; }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LimiterConfig<?> that = (LimiterConfig<?>) o;
        return rates.equals(that.rates) && matcher.equals(that.matcher) && matchers
                .equals(that.matchers) && ticker.equals(that.ticker);
    }

    @Override public int hashCode() {
        return Objects.hash(rates, matcher, matchers, ticker);
    }

    @Override public String toString() {
        return "LimiterConfig{source=" + source + ", rates=" + rates + ", matcher=" + matcher +
                ", matchers=" + matchers + ", ticker=" + ticker + '}';
    }
}
