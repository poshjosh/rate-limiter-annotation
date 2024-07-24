package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.expression.ExpressionMatchers;
import io.github.poshjosh.ratelimiter.model.RateConfig;

final class DefaultMatcherProvider<INPUT> extends AbstractMatcherProvider<INPUT> {

    DefaultMatcherProvider() {
        super(ExpressionMatchers.ofDefaults());
    }

    @Override
    public Matcher<INPUT> createMainMatcher(RateConfig rateConfig) {
        final Matcher<INPUT> expressionMatcher = createExpressionMatcher(
                rateConfig.getRates().getRateCondition()).orElse(null);
        if (isMatchNone(rateConfig, expressionMatcher != null)) {
            return Matchers.matchNone();
        }
        return andSourceMatcher(expressionMatcher, rateConfig);
    }
}
