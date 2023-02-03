package io.github.poshjosh.ratelimiter.expression;

import java.util.Objects;

final class DecimalExpressionResolver implements ExpressionResolver<Double> {
    DecimalExpressionResolver() {}
    @Override
    public boolean resolve(Expression<Double> expression) {
        if (!expression.getOperator().isNegation()) {
            return resolvePositive(expression);
        }
        return !resolvePositive(expression.flipOperator());
    }

    private boolean resolvePositive(Expression<Double> expression) {
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
        return Operator.Type.COMPARISON.equals(operator.getType());
    }
}
