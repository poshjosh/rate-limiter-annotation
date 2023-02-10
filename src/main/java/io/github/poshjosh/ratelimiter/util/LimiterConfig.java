package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.SleepingTicker;
import io.github.poshjosh.ratelimiter.annotation.RateSource;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.node.Node;

import java.util.*;

public final class LimiterConfig<R> {

    public static LimiterConfig<Object> of(Node<RateConfig> node) {
        return of(node, RateToBandwidthConverter.ofDefaults(),
                MatcherProvider.ofDefaults(), SleepingTicker.zeroOffset());
    }

    public static <R> LimiterConfig<R> of(
            Node<RateConfig> node,
            RateToBandwidthConverter rateToBandwidthConverter,
            MatcherProvider<R> matcherProvider,
            SleepingTicker sleepingTicker) {
        RateConfig rateConfig = Objects.requireNonNull(node.getValueOrDefault(null));
        Rates rates = rateConfig.getRates();
        Bandwidth[] bandwidths = rateToBandwidthConverter
                .convert(node.getName(), rates, sleepingTicker.elapsedMicros());
        Matcher<R> matcher = matcherProvider.createMatcher(node);
        List<Matcher<R>> matchers = matcherProvider.createMatchers(node);
        return new LimiterConfig<>(
                rateConfig.getSource(), rates, bandwidths, matcher, matchers, sleepingTicker);
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

    private final SleepingTicker sleepingTicker;

    private LimiterConfig(RateSource source, Rates rates, Bandwidth[] bandwidths,
            Matcher<R> matcher, List<Matcher<R>> matchers, SleepingTicker sleepingTicker) {
        this.source = Objects.requireNonNull(source);
        this.rates = Rates.of(rates);
        this.bandwidths = Arrays.copyOf(bandwidths, bandwidths.length);
        this.matcher = Objects.requireNonNull(matcher);
        this.matchers = Collections.unmodifiableList(new ArrayList<>(matchers));
        this.sleepingTicker = Objects.requireNonNull(sleepingTicker);
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

    public SleepingTicker getSleepingTicker() { return sleepingTicker; }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LimiterConfig<?> that = (LimiterConfig<?>) o;
        return rates.equals(that.rates) && matcher.equals(that.matcher) && matchers
                .equals(that.matchers) && sleepingTicker.equals(that.sleepingTicker);
    }

    @Override public int hashCode() {
        return Objects.hash(rates, matcher, matchers, sleepingTicker);
    }

    @Override public String toString() {
        return "LimiterConfig{source=" + source + ", rates=" + rates + ", matcher=" + matcher +
                ", matchers=" + matchers + ", sleepingTicker=" + sleepingTicker + '}';
    }
}
