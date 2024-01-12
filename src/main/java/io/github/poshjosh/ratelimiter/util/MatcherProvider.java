package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.model.RateConfig;

import java.util.List;

public interface MatcherProvider<INPUT> {
    static <INPUT> MatcherProvider<INPUT> ofDefaults() {
        return new DefaultMatcherProvider<>();
    }
    Matcher<INPUT> createMainMatcher(RateConfig rateConfig);
    List<Matcher<INPUT>> createSubMatchers(RateConfig rateConfig);
}
