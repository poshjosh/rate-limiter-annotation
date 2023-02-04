package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.SleepingTicker;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.node.Node;

import java.util.*;

public final class LimiterConfig<R, K> {

    public static LimiterConfig<Object, String> ofDefaults(Node<RateConfig> node) {
        return of(node, RateToBandwidthConverter.ofDefaults(),
                MatcherProvider.ofDefaults(), SleepingTicker.zeroOffset());
    }

    public static <R, K> LimiterConfig<R, K> of(
            Node<RateConfig> node,
            RateToBandwidthConverter rateToBandwidthConverter,
            MatcherProvider<R, K> matcherProvider,
            SleepingTicker sleepingTicker) {
        RateConfig rateConfig = Objects.requireNonNull(node.getValueOrDefault(null));
        Rates rates = rateConfig.getRates();
        Bandwidth[] bandwidths = rateToBandwidthConverter
                .convert(node.getName(), rates, sleepingTicker.elapsedMicros());
        Matcher<R, K> matcher = matcherProvider.createMatcher(node);
        List<Matcher<R, K>> matchers = matcherProvider.createMatchers(node);
        return new LimiterConfig<>(
                rateConfig.getSourceType(), rates, bandwidths, matcher, matchers, sleepingTicker);
    }

    private final SourceType sourceType;

    private final Rates rates;

    private final Bandwidth [] bandwidths;

    /**
     * The matcher to apply before applying individual matchers. This is usually
     * the path pattern matcher (and then) any matchers for general rate conditions.
     */
    private final Matcher<R, K> matcher;

    /**
     * Matchers for rate conditions specific to each rate.
     */
    private final List<Matcher<R, K>> matchers;

    private final SleepingTicker sleepingTicker;

    private LimiterConfig(SourceType sourceType, Rates rates, Bandwidth[] bandwidths,
            Matcher<R, K> matcher, List<Matcher<R, K>> matchers, SleepingTicker sleepingTicker) {
        this.sourceType = Objects.requireNonNull(sourceType);
        this.rates = Rates.of(rates);
        this.bandwidths = Arrays.copyOf(bandwidths, bandwidths.length);
        this.matcher = Objects.requireNonNull(matcher);
        this.matchers = Collections.unmodifiableList(new ArrayList<>(matchers));
        this.sleepingTicker = Objects.requireNonNull(sleepingTicker);
    }

    public SourceType getSourceType() { return sourceType; }

    public Rates getRates() {
        return rates;
    }

    public Bandwidth [] getBandwidths() { return bandwidths; }

    public Matcher<R, K> getMatcher() { return matcher; }

    public List<Matcher<R, K>> getMatchers() {
        return matchers;
    }

    public SleepingTicker getSleepingTicker() { return sleepingTicker; }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LimiterConfig<?, ?> that = (LimiterConfig<?, ?>) o;
        return rates.equals(that.rates) && Arrays.equals(bandwidths, that.bandwidths) && matcher
                .equals(that.matcher) && matchers.equals(that.matchers) && sleepingTicker
                .equals(that.sleepingTicker);
    }

    @Override public int hashCode() {
        int result = Objects.hash(rates, matcher, matchers, sleepingTicker);
        result = 31 * result + Arrays.hashCode(bandwidths);
        return result;
    }

    @Override public String toString() {
        return "LimiterConfig{type=" + sourceType + ", rates=" + rates + ", matcher=" + matcher +
                ", matchers=" + matchers + ", sleepingTicker=" + sleepingTicker + '}';
    }
}
