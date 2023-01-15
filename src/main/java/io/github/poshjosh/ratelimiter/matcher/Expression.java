package io.github.poshjosh.ratelimiter.matcher;

import java.util.Objects;

public final class Expression<T> {
    public static Expression<String> ofLenient(String expression) {
        final String [] parts = Splitter.ofExpression().lenient().split(expression);
        return of(parts[0], parts[1], parts[2]);
    }
    public static Expression<String> of(String expression) {
        final String [] parts = Splitter.ofExpression().split(expression);
        return of(parts[0], parts[1], parts[2]);
    }
    public static <T> Expression<T> of(T left, String operator, T right) {
        return of(left, Operator.of(operator), right);
    }
    public static <T> Expression<T> of(T left, Operator operator, T right) {
        return new Expression<>(left, operator, right);
    }
    private final T left;
    private final Operator operator;
    private final T right;
    private Expression(T left, Operator operator, T right) {
        this.left = Objects.requireNonNull(left);
        this.operator = Objects.requireNonNull(operator);
        this.right = Objects.requireNonNull(right);
    }
    public <U> Expression<U> with(U left, U right) {
        return Expression.of(left, operator, right);
    }
    public Expression<T> flipOperator() {
        return Expression.of(left, operator.flip(), right);
    }
    public T getLeft() {
        return left;
    }
    public Operator getOperator() {
        return operator;
    }
    public T getRight() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Expression<?> that = (Expression<?>) o;
        return left.equals(that.left) && operator.equals(that.operator) && right.equals(that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, operator, right);
    }

    public String getId() {
        return left + operator.getValue() + right;
    }

    @Override
    public String toString() {
        return "{" + left + operator.getValue() + right + '}';
    }
}
