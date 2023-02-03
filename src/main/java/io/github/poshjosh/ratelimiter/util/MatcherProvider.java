package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.node.Node;

import java.util.List;

public interface MatcherProvider<R, K> {
    static <R> MatcherProvider<R, String> ofDefaults() {
        return new DefaultMatcherProvider<>();
    }
    Matcher<R, K> createMatcher(Node<RateConfig> node);
    List<Matcher<R, K>> createMatchers(Node<RateConfig> node);
}
