package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.bandwidths.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.model.Rates;
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
        Matcher<K> mainMatcher;
        List<Matcher<K>> subMatchers;
        if(!hasLimitsInTree(node)) {
            LOG.debug("No limits specified for group, so no matcher will be created for: {}",
                    node.getName());
            mainMatcher = Matcher.matchNone();
            subMatchers = Collections.emptyList();
        } else {
            mainMatcher = matcherProvider.createMainMatcher(rateConfig);
            subMatchers = matcherProvider.createSubMatchers(rateConfig);
        }
        final LimiterConfig<K> limiterConfig =
                of(rateConfig.getSource(), rates, bandwidths, mainMatcher, subMatchers, ticker);
        LOG.trace("{}", limiterConfig);
        return limiterConfig;
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
        return node.requireValue().getRates().hasLimits();
    }

    public static <R> LimiterConfig<R> of(RateSource source, Rates rates, Bandwidth[] bandwidths,
            Matcher<R> matcher, List<Matcher<R>> matchers, Ticker ticker) {
        return new LimiterConfig<>(source, rates, bandwidths, matcher, matchers, ticker);
    }

    private final RateSource source;

    private final Rates rates;

    private final Bandwidth [] bandwidths;

    /**
     * The matcher to apply before applying sub matchers.
     */
    private final Matcher<R> mainMatcher;

    /**
     * Matchers for rate conditions specific to each rate.
     */
    private final List<Matcher<R>> subMatchers;

    private final Ticker ticker;

    private LimiterConfig(RateSource source, Rates rates, Bandwidth[] bandwidths,
            Matcher<R> mainMatcher, List<Matcher<R>> subMatchers, Ticker ticker) {
        this.source = Objects.requireNonNull(source);
        this.rates = Rates.of(rates);
        this.bandwidths = Arrays.copyOf(bandwidths, bandwidths.length);
        this.mainMatcher = Objects.requireNonNull(mainMatcher);
        this.subMatchers = Collections.unmodifiableList(new ArrayList<>(subMatchers));
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

    public Matcher<R> getMainMatcher() { return mainMatcher; }

    public List<Matcher<R>> getSubMatchers() {
        return subMatchers;
    }

    public Ticker getTicker() { return ticker; }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LimiterConfig<?> that = (LimiterConfig<?>) o;
        return rates.equals(that.rates) && mainMatcher.equals(that.mainMatcher) && subMatchers
                .equals(that.subMatchers) && ticker.equals(that.ticker);
    }

    @Override public int hashCode() {
        return Objects.hash(rates, mainMatcher, subMatchers, ticker);
    }

    @Override public String toString() {
        return "LimiterConfig{source=" + source + ", rates=" + rates + ", mainMatcher=" + mainMatcher +
                ", subMatchers=" + subMatchers + ", ticker=" + ticker + '}';
    }
}
