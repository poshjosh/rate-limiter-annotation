package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;

import java.util.*;
import java.util.stream.Collectors;

final class ExpressionMatcherProvider<R> implements MatcherProvider<R> {

    private static final class NodeNameMatcher<T> implements Matcher<T> {
        private final String name;
        private NodeNameMatcher(String name) {
            this.name = name;
        }
        @Override public String match(T target) {
            return Objects.equals(name, target) ? name : Matcher.NO_MATCH;
        }
        @Override public String toString() {
            return "NodeNameMatcher{" + "name='" + name + '\'' + '}';
        }
    }

    private final ExpressionMatcher<R, Object> expressionMatcher;

    ExpressionMatcherProvider() {
        expressionMatcher = ExpressionMatcher.ofDefault();
    }

    @Override public Matcher<R> createMatcher(RateConfig rateConfig) {
        final String expression = rateConfig.getRates().getRateCondition();
        return createMatcher(rateConfig.getId(), expression);
    }

    @Override public List<Matcher<R>> createMatchers(RateConfig rateConfig) {
        return createMatchers(rateConfig.getRates());
    }

    private Matcher<R> createMatcher(String node, String expression) {
        Matcher<R> matcher = new NodeNameMatcher<>(node);
        return createMatcher(expression).map(matcher::and).orElse(matcher);
    }

    private List<Matcher<R>> createMatchers(Rates rates) {
        return rates.getLimits().stream()
                .map(rate -> createMatcher(rate.getRateCondition()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Optional<Matcher<R>> createMatcher(String expression) {
        return expressionMatcher.matcher(expression);
    }

    @Override public String toString() {
        return "ExpressionMatcherProvider{expressionMatcher=" + expressionMatcher + '}';
    }
}
