package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.node.Node;

import java.util.*;
import java.util.stream.Collectors;

final class DefaultMatcherProvider<R> implements MatcherProvider<R, String> {

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

    private final ExpressionMatcher<R, Object> expressionMatcher;

    DefaultMatcherProvider() {
        expressionMatcher = ExpressionMatcher.ofDefault();
    }

    @Override public Matcher<R, String> createMatcher(Node<RateConfig> node) {
        final String expression = requireRateConfig(node).getRates().getRateCondition();
        return createMatcher(node.getName(), expression);
    }

    @Override public List<Matcher<R, String>> createMatchers(Node<RateConfig> node) {
        return createMatchers(requireRateConfig(node).getRates());
    }

    private RateConfig requireRateConfig(Node<RateConfig> node) {
        return node.getValueOptional().orElseThrow(RuntimeException::new);
    }

    private Matcher<R, String> createMatcher(String node, String expression) {
        if (expression == null || expression.isEmpty()) {
            return new NodeNameMatcher<>(node);
        }
        if (expressionMatcher.isSupported(expression)) {
            return expressionMatcher.with(expression);
        }
        return new NodeNameMatcher<>(node);
    }

    private List<Matcher<R, String>> createMatchers(Rates rates) {
        return rates.getLimits().stream()
                .map(rate -> createMatcher(rate.getRateCondition()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Optional<Matcher<R, String>> createMatcher(String expression) {
        if (expression == null || expression.isEmpty()) {
            return Optional.empty();
        }
        if (expressionMatcher.isSupported(expression)) {
            return Optional.of(expressionMatcher.with(expression));
        }
        return Optional.empty();
    }

    @Override public String toString() {
        return "DefaultMatcherProvider{expressionMatcher=" + expressionMatcher + '}';
    }
}
