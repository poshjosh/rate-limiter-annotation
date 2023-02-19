package io.github.poshjosh.ratelimiter.expression;

import java.util.Objects;

final class LongExpressionResolver implements ExpressionResolver<Long> {
    LongExpressionResolver() {}
    @Override
    public boolean resolve(Expression<Long> expression) {
        if (!expression.getOperator().isNegation()) {
            return resolvePositive(expression);
        }
        return !resolvePositive(expression.flipOperator());
    }

    private boolean resolvePositive(Expression<Long> expression) {
        final Operator operator = expression.getOperator();
        switch (operator.getSymbol()) {
            case "=":
                final Long left = expression.getLeftOrDefault(null);
                final Long right = expression.getRightOrDefault(null);
                return Objects.equals(left, right);
            case ">":
                return expression.requireLeft() > expression.requireRight();
            case ">=":
                return expression.requireLeft() >= expression.requireRight();
            case "<":
                return expression.requireLeft() < expression.requireRight();
            case "<=":
                return expression.requireLeft() <= expression.requireRight();
            default:
                throw Checks.notSupported(this, operator);
        }
    }

    @Override
    public boolean isSupported(Operator operator) {
        return operator.isType(Operator.Type.COMPARISON);
    }
}
