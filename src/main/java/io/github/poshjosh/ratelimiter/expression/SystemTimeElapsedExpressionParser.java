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
            return expression.with(System.currentTimeMillis() - TIME_AT_STARTUP, right(expression));
        }
        throw Checks.notSupported(this, lhs);
    }

    private Long right(Expression<String> expression) {
        final String lhs = expression.requireLeft();
        if (TIME_ELAPSED.equals(lhs)) {
            return Duration.parse(expression.requireRight()).toMillis();
        }
        throw Checks.notSupported(this, lhs);
    }
}
