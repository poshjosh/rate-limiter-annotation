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
        switch (operator.getSymbol()) {
            case "=":
                final Double left = expression.getLeftOrDefault(null);
                final Double right = expression.getRightOrDefault(null);
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
