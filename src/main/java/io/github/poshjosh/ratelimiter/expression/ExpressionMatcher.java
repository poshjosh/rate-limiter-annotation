package io.github.poshjosh.ratelimiter.expression;

import io.github.poshjosh.ratelimiter.Operator;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

public interface ExpressionMatcher<R, T> extends Matcher<R> {

    ExpressionMatcher<Object, Object> MATCH_NONE = new ExpressionMatcher<Object, Object>() {
        @Override public String match(Object request) { return Matcher.NO_MATCH; }
        @Override public ExpressionMatcher<Object, Object> matcher(Expression<String> expression) {
            return this;
        }
        @Override public boolean isSupported(Expression<String> expression) {
            return true;
        }
    };

    @SuppressWarnings("unchecked")
    static <T, K> ExpressionMatcher<T, K> matchNone() {
        return (ExpressionMatcher<T, K>)MATCH_NONE;
    }

    static <R> ExpressionMatcher<R, Object> ofDefault() {
        return any(ofSystemMemory(), ofSystemTime(), ofSystemTimeElapsed(),
                ofJvmThread(), ofSystemProperty(), ofSystemEnvironment());
    }

    static <R> ExpressionMatcher<R, Object> any(ExpressionMatcher<R, ?>... matchers) {
        return new AnyExpressionMatcher<>(matchers);
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

    static <R> ExpressionMatcher<R, String> ofSystemProperty() {
        return of(ExpressionParser.ofSystemProperty(),
                ExpressionResolver.ofString(),
                SystemPropertyExpressionParser.LHS+"=");
    }


    static <R> ExpressionMatcher<R, String> ofSystemEnvironment() {
        return of(ExpressionParser.ofSystemEnvironment(),
                ExpressionResolver.ofString(),
                SystemEnvironmentExpressionParser.LHS+"=");
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
        return of(expressionParser, expressionResolver, Expression.of(sampleExpression));
    }

    static <R, T> ExpressionMatcher<R, T> of(
            ExpressionParser<R, T> expressionParser,
            ExpressionResolver<T> expressionResolver,
            Expression<String> sampleExpression) {
        return new DefaultExpressionMatcher<>(expressionParser, expressionResolver, sampleExpression);
    }

    @Override String match(R request);

    ExpressionMatcher<R, T> matcher(Expression<String> expression);

    boolean isSupported(Expression<String> expression);

    default Optional<Matcher<R>> matcher(String text) {
        if (!StringUtils.hasText(text)) {
            return Optional.empty();
        }
        String [] parts = Splitter.splitIntoExpressionsAndConjunctors(text);
        if (parts.length == 0) {
            return Optional.empty();
        }
        Matcher<R> result = null;
        Operator operator = Operator.NONE;
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            if (i % 2 == 0) {
                final Matcher<R> matcher;
                if (!StringUtils.hasText(part)) {
                    matcher = null;
                } else {
                    Expression<String> expression = Expression.of(part);
                    if (isSupported(expression)) {
                        matcher = matcher(expression);
                    } else {
                        throw Checks.notSupported(this, "expression: " + part);
                    }
                }
                if (result == null) {
                    result = matcher;
                } else {
                    switch(operator) {
                        case AND:
                            result = result.and(matcher); break;
                        case OR:
                            result = result.or(matcher); break;
                        case NONE:
                        default:
                            throw Checks.notSupported(this, "operator: " + operator);
                    }
                }
            } else {
                operator = Operator.ofSymbol(parts[i]);
            }
        }
        return Optional.ofNullable(result);
    }
}
