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
        switch (expression.getOperator().getValue()) {
            case "=":
                return Objects.equals(expression.getLeft(), expression.getRight());
            case "^":
                return expression.getLeft().startsWith(expression.getRight());
            case "$":
                return expression.getLeft().endsWith(expression.getRight());
            case "%":
                return expression.getLeft().contains(expression.getRight());
            default:
                throw Checks.notSupported(this, expression.getOperator());
        }
    }

    @Override
    public boolean isSupported(Operator operator) {
        return Operator.Type.STRING.equals(operator.getType());
    }
}
