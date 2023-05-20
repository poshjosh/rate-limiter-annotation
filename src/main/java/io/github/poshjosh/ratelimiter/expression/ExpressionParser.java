package io.github.poshjosh.ratelimiter.expression;

import java.time.LocalDateTime;

/**
 * Parses expression of one type to another type
 * @param <S> The type of the source e.g a web request (web) or a system (sys) etc
 * @param <T> The type of the resulting expression
 */
public interface ExpressionParser<S, T> {

    long TIME_AT_STARTUP = System.currentTimeMillis();

    static <S> ExpressionParser<S, Long> ofSystemMemory() {
        return new JvmMemoryExpressionParser<>();
    }

    static <S> ExpressionParser<S, LocalDateTime> ofSystemTime() {
        return new SystemTimeExpressionParser<>();
    }

    static <S> ExpressionParser<S, Long> ofSystemTimeElapsed() {
        return new SystemTimeElapsedExpressionParser<>();
    }

    static <S> ExpressionParser<S, String> ofSystemProperty() {
        return new SystemPropertyExpressionParser<>();
    }


    static <S> ExpressionParser<S, String> ofSystemEnvironment() {
        return new SystemEnvironmentExpressionParser<>();
    }

    static <S> ExpressionParser<S, Object> ofJvmThread() {
        return new JvmThreadExpressionParser<>();
    }

    /**
     * @param expression the expression to check if supported
     * @return true if the provided expression is supported
     * @see #isSupported(Expression <String>)
     */
    default boolean isSupported(String expression) {
        return isSupported(Expression.of(expression));
    }

    /**
     * @param expression the expression to check if supported
     * @return true if the provided expression is supported
     */
    boolean isSupported(Expression<String> expression);

    /**
     * Parse a string expression into another type of expression
     * @return the result of parsing a string expression into another type
     */
    Expression<T> parse(S source, Expression<String> expression);
}
