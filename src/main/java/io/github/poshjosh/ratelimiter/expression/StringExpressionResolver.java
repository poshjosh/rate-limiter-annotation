package io.github.poshjosh.ratelimiter.expression;

import java.util.Objects;

class StringExpressionResolver implements ExpressionResolver<String>{
    StringExpressionResolver() {}

    @Override
    public boolean resolve(Expression<String> expression) {
        if (!expression.getOperator().isNegation()) {
            return resolvePositive(expression);
        }
        return !resolvePositive(expression.flipOperator());
    }

    private boolean resolvePositive(Expression<String> expression) {
        switch (expression.getOperator().getSymbol()) {
            case "=":
                final String left = expression.getLeftOrDefault(null);
                final String right = expression.getRightOrDefault(null);
                return Objects.equals(left, right);
            case "^":
                return expression.requireLeft().startsWith(expression.requireRight());
            case "$":
                return expression.requireLeft().endsWith(expression.requireRight());
            case "%":
                return expression.requireLeft().contains(expression.requireRight());
            default:
                throw Checks.notSupported(this, expression.getOperator());
        }
    }

    @Override
    public boolean isSupported(Operator operator) {
        return Operator.Type.STRING.equals(operator.getType());
    }
}
