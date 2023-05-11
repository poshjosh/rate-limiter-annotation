package io.github.poshjosh.ratelimiter.expression;

import java.time.Duration;

final class SystemTimeElapsedExpressionParser<S> implements ExpressionParser<S, Long> {

    public static final String TIME_ELAPSED = "sys.time.elapsed";

    SystemTimeElapsedExpressionParser() {}

    @Override
    public boolean isSupported(Expression<String> expression) {
        final String lhs = expression.requireLeft();
        if (TIME_ELAPSED.equals(lhs)) {
            return expression.getOperator().isType(Operator.Type.COMPARISON);
        }
        return false;
    }

    @Override
    public Expression<Long> parse(S source, Expression<String> expression) {

        final String lhs = expression.requireLeft();
        if (TIME_ELAPSED.equals(lhs)) {
            return expression.with(System.currentTimeMillis() - getStartTime(source), right(expression));
        }
        throw Checks.notSupported(this, lhs);
    }

    private long getStartTime(S source) {
        if (source instanceof Long) {
            return (Long)source;
        }
        if (source instanceof String) {
            try {
                return Long.parseLong((String)source);
            } catch (NumberFormatException e) {
                return TIME_AT_STARTUP;
            }
        }
        return TIME_AT_STARTUP;
    }

    private Long right(Expression<String> expression) {
        final String lhs = expression.requireLeft();
        if (TIME_ELAPSED.equals(lhs)) {
            return Duration.parse(expression.requireRight()).toMillis();
        }
        throw Checks.notSupported(this, lhs);
    }
}
