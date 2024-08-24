package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.matcher.Matcher;
import io.github.poshjosh.ratelimiter.matcher.Matchers;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractMatcherProvider<INPUT> implements MatcherProvider<INPUT> {

    private final ExpressionMatcher<INPUT> expressionMatcher;

    protected AbstractMatcherProvider(ExpressionMatcher<INPUT> expressionMatcher) {
        this.expressionMatcher = Objects.requireNonNull(expressionMatcher);
    }

    @Override
    public List<Matcher<INPUT>> createSubMatchers(RateConfig rateConfig) {
        List<Rate> subRates = rateConfig.getRates().getRates();
        if (subRates.isEmpty()) {
            return Collections.emptyList();
        }
        if (subRates.size() == 1) {
            return createExpressionMatcher(subRates.get(0).getCondition())
                    .map(Collections::singletonList)
                    // Tag:Rule:number-of-matchers-must-equal-number-of-rates
                    .orElse(Collections.singletonList(Matchers.matchNone()));
        }
        return subRates.stream()
                .map(rate -> createExpressionMatcher(rate.getCondition()).orElse(Matchers.matchNone()))
                .collect(Collectors.toList());
    }

    protected Optional<Matcher<INPUT>> createExpressionMatcher(String expression) {
        if (expression == null || expression.isEmpty()) {
            return Optional.empty();
        }
        return expressionMatcher.matcher(expression);
    }

    protected boolean isMatchNone(RateConfig rateConfig, boolean isExpressionPresent) {
        return !isExpressionPresent
                && !rateConfig.getRates().isSet()
                && !rateConfig.isGroupType();
    }

    protected Matcher<INPUT> andSourceMatcher(Matcher<INPUT> matcher, RateConfig rateConfig) {
        final Matcher<INPUT> sourceMatcher = new RateSourceMatcher<>(
                rateConfig.getId(), rateConfig.getSource().getSource());
        return matcher == null ? sourceMatcher : matcher.and(sourceMatcher);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{expressionMatcher=" + expressionMatcher + '}';
    }
}
