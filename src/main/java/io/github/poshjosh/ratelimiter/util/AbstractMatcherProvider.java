package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
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
    public List<Matcher<INPUT>> createLimitMatchers(RateConfig rateConfig) {
        List<Rate> subLimits = rateConfig.getRates().getSubLimits();
        if (subLimits.isEmpty()) {
            return Collections.emptyList();
        }
        if (subLimits.size() == 1) {
            return createExpressionMatcher(subLimits.get(0).getRateCondition())
                    .map(Collections::singletonList)
                    // Tag:Rule:number-of-matchers-must-equal-number-of-rates
                    .orElse(Collections.singletonList(Matchers.matchNone()));
        }
        return subLimits.stream()
                .map(rate -> createExpressionMatcher(rate.getRateCondition()).orElse(Matchers.matchNone()))
                .collect(Collectors.toList());
    }

    protected Optional<Matcher<INPUT>> createExpressionMatcher(String expression) {
        return expressionMatcher.matcher(expression);
    }

    protected boolean isMatchNone(RateConfig rateConfig, boolean isExpressionPresent) {
        return !isExpressionPresent
                && !rateConfig.getRates().hasLimitsSet()
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
