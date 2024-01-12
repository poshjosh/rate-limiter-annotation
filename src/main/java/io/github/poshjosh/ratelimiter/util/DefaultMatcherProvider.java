package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.Rates;

import java.util.*;
import java.util.stream.Collectors;

final class DefaultMatcherProvider<INPUT> implements MatcherProvider<INPUT> {

    private final ExpressionMatcher<INPUT, Object> expressionMatcher;

    DefaultMatcherProvider() {
        expressionMatcher = ExpressionMatcher.ofDefault();
    }

    @Override public Matcher<INPUT> createMainMatcher(RateConfig rateConfig) {
        final String expression = rateConfig.getRates().getRateCondition();
        final Matcher<INPUT> matcher = new RateSourceMatcher<>(rateConfig.getId());
        return createExpressionMatcher(expression).map(matcher::and).orElse(matcher);
    }

    @Override public List<Matcher<INPUT>> createSubMatchers(RateConfig rateConfig) {
        return rateConfig.getRates().getLimits().stream()
                .map(rate -> createExpressionMatcher(rate.getRateCondition()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Optional<Matcher<INPUT>> createExpressionMatcher(String expression) {
        return expressionMatcher.matcher(expression);
    }

    @Override public String toString() {
        return "DefaultMatcherProvider{expressionMatcher=" + expressionMatcher + '}';
    }
}
