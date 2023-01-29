package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.matcher.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.RateConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class DefaultMatcherProvider<T> implements MatcherProvider<T> {

    private static final class NodeNameMatcher<T> implements Matcher<T, String> {
        private final String name;
        private NodeNameMatcher(String name) {
            this.name = name;
        }
        @Override public String matchOrNull(T target) {
            return Objects.equals(name, target) ? name : null;
        }
        @Override public String toString() {
            return "NodeNameMatcher{" + "name='" + name + '\'' + '}';
        }
    }

    private final Map<String, Matcher<T, ?>> nameToMatcher = new HashMap<>();

    private final ExpressionMatcher<T, Object> expressionMatcher;

    DefaultMatcherProvider() {
        expressionMatcher = ExpressionMatcher.ofDefault();
    }

    @Override public Matcher<T, ?> getMatcher(String node, RateConfig rateConfig) {
        final String expression = rateConfig.getValue().getRateCondition();
        return nameToMatcher.computeIfAbsent(node, k -> createMatcher(node, expression));
    }

    private Matcher<T, ?> createMatcher(String node, String expression) {
        if (expression == null || expression.isEmpty()) {
            return new NodeNameMatcher<>(node);
        }
        if (expressionMatcher.isSupported(expression)) {
            return expressionMatcher.with(expression);
        } else {
            return new NodeNameMatcher<>(node);
        }
    }
}
