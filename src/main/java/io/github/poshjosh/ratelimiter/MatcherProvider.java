package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.RateConfig;

public interface MatcherProvider<T> {
    static <T> MatcherProvider<T> ofDefaults() {
        return new DefaultMatcherProvider<>();
    }
    Matcher<T, ?> getMatcher(String name, RateConfig rateConfig);
}
