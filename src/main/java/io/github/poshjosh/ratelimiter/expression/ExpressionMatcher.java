package io.github.poshjosh.ratelimiter.expression;

import io.github.poshjosh.ratelimiter.util.Matcher;

import java.time.LocalDateTime;

public interface ExpressionMatcher<R, T> extends Matcher<R, String> {

    ExpressionMatcher<Object, Object> MATCH_NONE = new ExpressionMatcher<Object, Object>() {
        @Override public String matchOrNull(Object request) {
            return null;
        }
        @Override public ExpressionMatcher<Object, Object> with(Expression<String> expression) {
            return this;
        }
        @Override public boolean isSupported(Expression<String> expression) {
            return false;
        }
    };

    @SuppressWarnings("unchecked")
    static <T, K> ExpressionMatcher<T, K> matchNone() {
        return (ExpressionMatcher<T, K>)MATCH_NONE;
    }

    static <R> ExpressionMatcher<R, Object> ofDefault() {
        return of(ofSystemMemory(), ofSystemTime(), ofSystemTimeElapsed(), ofJvmThread());
    }

    static <R> ExpressionMatcher<R, Object> of(ExpressionMatcher<R, ?>... matchers) {
        return new ExpressionMatcherComposite<>(matchers);
    }

    static <R> ExpressionMatcher<R, Long> ofSystemMemory() {
        return of(ExpressionParser.ofSystemMemory(),
                ExpressionResolver.ofLong(),
                SystemMemoryExpressionParser.MEMORY_MAX+"=");
    }

    static <R> ExpressionMatcher<R, LocalDateTime> ofSystemTime() {
        return of(ExpressionParser.ofSystemTime(),
                ExpressionResolver.ofDateTime(),
                SystemTimeExpressionParser.TIME+"=");
    }

    static <R> ExpressionMatcher<R, Long> ofSystemTimeElapsed() {
        return of(ExpressionParser.ofSystemTimeElapsed(),
                ExpressionResolver.ofLong(),
                SystemTimeElapsedExpressionParser.TIME_ELAPSED+"=");
    }

    static <R> ExpressionMatcher<R, Object> ofJvmThread() {
        return of(ExpressionParser.ofJvmThread(),
                ExpressionResolver.ofJvmThread(),
                JvmThreadExpressionParser.COUNT+"=");
    }

    static <R, T> ExpressionMatcher<R, T> of(
            ExpressionParser<R, T> expressionParser,
            ExpressionResolver<T> expressionResolver,
            String sampleExpression) {
        return of(expressionParser, expressionResolver, Expression.ofLenient(sampleExpression));
    }

    static <R, T> ExpressionMatcher<R, T> of(
            ExpressionParser<R, T> expressionParser,
            ExpressionResolver<T> expressionResolver,
            Expression<String> sampleExpression) {
        return new DefaultExpressionMatcher<>(expressionParser, expressionResolver, sampleExpression);
    }

    @Override String matchOrNull(R request);

    default ExpressionMatcher<R, T> with(String expression) {
        return with(Expression.of(expression));
    }

    ExpressionMatcher<R, T> with(Expression<String> expression);

    default boolean isSupported(String expression) {
        return isSupported(Expression.ofLenient(expression));
    }

    boolean isSupported(Expression<String> expression);
}
