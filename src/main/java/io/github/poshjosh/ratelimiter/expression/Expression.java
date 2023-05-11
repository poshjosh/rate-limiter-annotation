package io.github.poshjosh.ratelimiter.expression;

public interface Expression<T> {
    Expression<Object> TRUE = of(null, Operator.EQUALS, null);
    Expression<Object> FALSE = TRUE.flipOperator();

    static Expression<String> of(String expression) {
        final String [] parts = Splitter.EXPRESSION_SPLITTER.split(expression);
        return of(parts[0], parts[1], parts[2]);
    }
    static <T> Expression<T> of(T left, String operator, T right) {
        return of(left, Operator.of(operator), right);
    }
    static <T> Expression<T> of(T left, Operator operator, T right) {
        return new DefaultExpression<>(left, operator, right);
    }
    default Expression<String> requireRightAsExpression() {
        final String rhs = requireRight().toString();
        return Expression.of(StringUtil.without(rhs, "{", "}"));
    }
    <U> Expression<U> with(U left, U right);
    Expression<T> flipOperator();
    T requireLeft();
    T getLeftOrDefault(T resultIfNone);
    Operator getOperator();
    T requireRight();
    T getRightOrDefault(T resultIfNone);
    String getId();
}
