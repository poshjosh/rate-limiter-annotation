package io.github.poshjosh.ratelimiter.matcher;

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
        switch (operator.getValue()) {
            case "=":
                return Objects.equals(expression.getLeft(), expression.getRight());
            case ">":
                return expression.getLeft() > expression.getRight();
            case ">=":
                return expression.getLeft() >= expression.getRight();
            case "<":
                return expression.getLeft() < expression.getRight();
            case "<=":
                return expression.getLeft() <= expression.getRight();
            default:
                throw Checks.notSupported(this, operator);
        }
    }

    @Override
    public boolean isSupported(Operator operator) {
        return Operator.OperatorType.COMPARISON.equals(operator.getType());
    }
}
