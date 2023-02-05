package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.node.Node;

import java.util.*;
import java.util.stream.Collectors;

final class DefaultMatcherProvider<R> implements MatcherProvider<R> {

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

    DefaultMatcherProvider() {
        expressionMatcher = ExpressionMatcher.ofDefault();
    }

    @Override public Matcher<R> createMatcher(Node<RateConfig> node) {
        final String expression = requireRateConfig(node).getRates().getRateCondition();
        return createMatcher(node.getName(), expression);
    }

    @Override public List<Matcher<R>> createMatchers(Node<RateConfig> node) {
        return createMatchers(requireRateConfig(node).getRates());
    }

    private RateConfig requireRateConfig(Node<RateConfig> node) {
        return node.getValueOptional().orElseThrow(RuntimeException::new);
    }

    private Matcher<R> createMatcher(String node, String expression) {
        Matcher<R> matcher = new NodeNameMatcher<>(node);
        return createMatcher(expression).map(matcher::andThen).orElse(matcher);
    }

    private List<Matcher<R>> createMatchers(Rates rates) {
        return rates.getLimits().stream()
                .map(rate -> createMatcher(rate.getRateCondition()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Optional<Matcher<R>> createMatcher(String expression) {
        if (expression == null || expression.isEmpty()) {
            return Optional.empty();
        }
        if (expressionMatcher.isSupported(expression)) {
            return Optional.of(expressionMatcher.with(expression));
        }
        throw new UnsupportedOperationException(
                expressionMatcher.getClass().getSimpleName() + " does not support: " + expression);
    }

    @Override public String toString() {
        return "DefaultMatcherProvider{expressionMatcher=" + expressionMatcher + '}';
    }
}
