package io.github.poshjosh.ratelimiter.expression;

import java.time.LocalDateTime;

final class SystemTimeExpressionParser<S> implements ExpressionParser<S, LocalDateTime> {

    public static final String TIME = "sys.time";

    SystemTimeExpressionParser() {}

    @Override
    public boolean isSupported(Expression<String> expression) {
        final String lhs = expression.requireLeft();
        if (TIME.equals(lhs)) {
            return Operator.Type.COMPARISON.equals(expression.getOperator().getType());
        }
        return false;
    }

    @Override
    public Expression<LocalDateTime> parse(S source, Expression<String> expression) {
        final String lhs = expression.requireLeft();
        if (TIME.equals(lhs)) {
            return expression.with(LocalDateTime.now(), right(expression));
        }
        throw Checks.notSupported(this, lhs);
    }

    private LocalDateTime right(Expression<String> expression) {
        final String lhs = expression.requireLeft();
        if (TIME.equals(lhs)) {
            return LocalDateTime.parse(expression.requireRight());
        }
        throw Checks.notSupported(this, lhs);
    }
}
