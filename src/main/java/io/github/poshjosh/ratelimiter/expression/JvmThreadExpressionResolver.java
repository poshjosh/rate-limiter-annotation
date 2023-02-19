package io.github.poshjosh.ratelimiter.expression;

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
        if (expression.requireLeft() instanceof Long) {
            return longExpressionResolver.resolve((Expression<Long>)expression);
        } else {
            return Objects.equals(
                    expression.getLeftOrDefault(null),
                    expression.getRightOrDefault(null));
        }
    }

    @Override
    public boolean isSupported(Operator operator) {
        return operator.isType(Operator.Type.COMPARISON);
    }
}
