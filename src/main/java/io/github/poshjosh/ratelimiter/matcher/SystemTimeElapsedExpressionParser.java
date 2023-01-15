package io.github.poshjosh.ratelimiter.matcher;

import java.time.Duration;

final class SystemTimeElapsedExpressionParser<S> implements ExpressionParser<S, Long> {

    private static final long TIME_AT_STARTUP = System.currentTimeMillis();

    public static final String TIME_ELAPSED = "sys.time.elapsed";

    SystemTimeElapsedExpressionParser() {}

    @Override
    public boolean isSupported(Expression<String> expression) {
        final String lhs = expression.getLeft();
        if (TIME_ELAPSED.equals(lhs)) {
            return Operator.OperatorType.COMPARISON.equals(expression.getOperator().getType());
        }
        return false;
    }

    @Override
    public Expression<Long> parse(S source, Expression<String> expression) {
        final String lhs = expression.getLeft();
        if (TIME_ELAPSED.equals(lhs)) {
            return expression.with(System.currentTimeMillis() - TIME_AT_STARTUP, right(expression));
        }
        throw Checks.notSupported(this, lhs);
    }

    private Long right(Expression<String> expression) {
        final String lhs = expression.getLeft();
        if (TIME_ELAPSED.equals(lhs)) {
            return Duration.parse(expression.getRight()).toMillis();
        }
        throw Checks.notSupported(this, lhs);
    }
}
