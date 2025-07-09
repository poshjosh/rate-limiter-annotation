package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.expression.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.matcher.Matcher;
import io.github.poshjosh.ratelimiter.matcher.Matchers;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.store.Store;
import io.github.poshjosh.ratelimiter.store.Stores;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractMatcherProvider<INPUT> implements MatcherProvider<INPUT> {

    private final ExpressionMatcher<INPUT> expressionMatcher;

    private final Store<String, Matcher<INPUT>> matchersStore;

    protected AbstractMatcherProvider(ExpressionMatcher<INPUT> expressionMatcher) {
        this.expressionMatcher = Objects.requireNonNull(expressionMatcher);

        // We decided no need for thread safety for the cache. This is because
        // it is ok to override an existing value with a new value as we only
        // use this cache to prevent creating the same matcher multiple times.

        // TOD0: Try this
        // TODO: Make the capacity of the matchers cache configurable
//        this.matchersStore = Stores.ofLRU(10_000, 0.75f);
        this.matchersStore = Stores.noop();
    }

    @Override
    public List<Matcher<INPUT>> createSubMatchers(RateConfig rateConfig) {
        List<Rate> subRates = rateConfig.getRates().getRates();
        if (subRates.isEmpty()) {
            return Collections.emptyList();
        }
        if (subRates.size() == 1) {
            Matcher<INPUT> matcher =
                    expressionMatcherOrFallback(
                            subRates.get(0).getCondition(), Matchers.matchNone());
            // Tag:Rule:number-of-matchersStore-must-equal-number-of-rates
            return Collections.singletonList(matcher);
        }
        return subRates.stream()
                .map(rate -> expressionMatcherOrFallback(rate.getCondition(), Matchers.matchNone()))
                .collect(Collectors.toList());
    }

    //@Nullable
    protected Matcher<INPUT> expressionMatcherOrFallback(
            String expression, Matcher<INPUT> fallback) {
        if (expression == null || expression.isEmpty()) {
            return fallback;
        }

        Matcher<INPUT> result = matchersStore.get(expression);
        if (result != null) {
            return result;
        }

        result = expressionMatcher.matcherOrFallback(expression, fallback);
        matchersStore.put(expression, result);
        return result;
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
