package io.github.poshjosh.ratelimiter.matcher;

import java.time.LocalDateTime;

class DateTimeExpressionResolver implements ExpressionResolver<LocalDateTime>{
    DateTimeExpressionResolver() {}

    @Override
    public boolean resolve(Expression<LocalDateTime> expression) {
        if (!expression.getOperator().isNegation()) {
            return resolvePositive(expression);
        }
        return !resolvePositive(expression.flipOperator());
    }

    private boolean resolvePositive(Expression<LocalDateTime> expression) {
        switch (expression.getOperator().getValue()) {
            case "=":
                return expression.getLeft().isEqual(expression.getRight());
            case ">":
                return expression.getLeft().isAfter(expression.getRight());
            case ">=":
                return expression.getLeft().isAfter(expression.getRight())
                        || expression.getLeft().isEqual(expression.getRight());
            case "<":
                return expression.getLeft().isBefore(expression.getRight());
            case "<=":
                return expression.getLeft().isBefore(expression.getRight())
                        || expression.getLeft().isEqual(expression.getRight());
            default:
                throw Checks.notSupported(this, expression.getOperator());
        }
    }

    @Override
    public boolean isSupported(Operator operator) {
        return Operator.OperatorType.COMPARISON.equals(operator.getType());
    }
}
