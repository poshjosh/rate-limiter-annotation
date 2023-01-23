package io.github.poshjosh.ratelimiter.matcher;

import java.util.Objects;

final class JvmThreadExpressionResolver implements ExpressionResolver<Object> {
    private final ExpressionResolver<Long> longExpressionResolver;
    JvmThreadExpressionResolver() {
        this.longExpressionResolver = ExpressionResolver.ofLong();
    }
    @Override
    public boolean resolve(Expression<Object> expression) {
        if (!expression.getOperator().isNegation()) {
            return resolvePositive(expression);
        }
        return !resolvePositive(expression.flipOperator());
    }

    private boolean resolvePositive(Expression<?> expression) {
        final Object lhs = expression.getLeft();
        if (lhs instanceof Long) {
            return longExpressionResolver.resolve((Expression<Long>)expression);
        } else {
            return Objects.equals(expression.getLeft(), expression.getRight());
        }
    }

    @Override
    public boolean isSupported(Operator operator) {
        return Operator.Type.COMPARISON.equals(operator.getType());
    }
}
