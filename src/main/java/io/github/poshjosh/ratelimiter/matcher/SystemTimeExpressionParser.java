package io.github.poshjosh.ratelimiter.matcher;

import java.time.LocalDateTime;

final class SystemTimeExpressionParser<S> implements ExpressionParser<S, LocalDateTime> {

    public static final String TIME = "sys.time";

    SystemTimeExpressionParser() {}

    @Override
    public boolean isSupported(Expression<String> expression) {
        final String lhs = expression.getLeft();
        if (TIME.equals(lhs)) {
            return Operator.OperatorType.COMPARISON.equals(expression.getOperator().getType());
        }
        return false;
    }

    @Override
    public Expression<LocalDateTime> parse(S source, Expression<String> expression) {
        final String lhs = expression.getLeft();
        if (TIME.equals(lhs)) {
            return expression.with(LocalDateTime.now(), right(expression));
        }
        throw Checks.notSupported(this, lhs);
    }

    private LocalDateTime right(Expression<String> expression) {
        final String lhs = expression.getLeft();
        if (TIME.equals(lhs)) {
            return LocalDateTime.parse(expression.getRight());
        }
        throw Checks.notSupported(this, lhs);
    }
}
