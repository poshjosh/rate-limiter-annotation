package io.github.poshjosh.ratelimiter.expression;

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
        switch (expression.getOperator().getSymbol()) {
            case "=":
                final LocalDateTime left = expression.getLeftOrDefault(null);
                final LocalDateTime right = expression.getRightOrDefault(null);
                if (left == null && right == null) {
                    return true;
                }
                if (left == null || right == null) {
                    return false;
                }
                return left.isEqual(right);
            case ">":
                return expression.requireLeft().isAfter(expression.requireRight());
            case ">=":
                return expression.requireLeft().isAfter(expression.requireRight())
                        || expression.requireLeft().isEqual(expression.requireRight());
            case "<":
                return expression.requireLeft().isBefore(expression.requireRight());
            case "<=":
                return expression.requireLeft().isBefore(expression.requireRight())
                        || expression.requireLeft().isEqual(expression.requireRight());
            default:
                throw Checks.notSupported(this, expression.getOperator());
        }
    }

    @Override
    public boolean isSupported(Operator operator) {
        return operator.isType(Operator.Type.COMPARISON);
    }
}
