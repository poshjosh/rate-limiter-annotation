package io.github.poshjosh.ratelimiter.expression;

abstract class AbstractStringMappingExpressionParser<S> implements ExpressionParser<S, String> {

    AbstractStringMappingExpressionParser() { }

    abstract String getLHS();
    abstract String getValue(String name);

    @Override
    public boolean isSupported(Expression<String> expression) {
        return getLHS().equals(expression.requireLeft()) &&
                expression.getOperator().equalsIgnoreNegation(Operator.EQUALS);
    }

    @Override
    public Expression<String> parse(S source, Expression<String> expression) {
        Expression<String> rhs = expression.requireRightAsExpression();
        String name = rhs.requireLeft();
        String value = rhs.getRightOrDefault(null);
        return rhs.with(getValue(name), value);
    }
}
