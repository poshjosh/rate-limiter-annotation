package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.node.Node;

import java.util.List;

public interface MatcherProvider<R> {
    static <R> MatcherProvider<R> ofDefaults() {
        return new DefaultMatcherProvider<>();
    }
    Matcher<R> createMatcher(RateConfig rateConfig);
    List<Matcher<R>> createMatchers(RateConfig rateConfig);
}
