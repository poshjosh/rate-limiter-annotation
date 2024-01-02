package io.github.poshjosh.ratelimiter.util;

import java.util.List;

public interface MatcherProvider<R> {
    static <R> MatcherProvider<R> ofDefaults() {
        return new ExpressionMatcherProvider<>();
    }
    Matcher<R> createMatcher(RateConfig rateConfig);
    List<Matcher<R>> createMatchers(RateConfig rateConfig);
}
