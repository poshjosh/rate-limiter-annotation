package io.github.poshjosh.ratelimiter.expression;

import java.util.Objects;

class DefaultExpression<T> implements Expression<T> {

    private final T left;
    private final Operator operator;
    private final T right;
    private final String id;

    DefaultExpression(T left, Operator operator, T right) {
        this.left = left;
        this.operator = Objects.requireNonNull(operator);
        this.right = right; // Nullable
        this.id = "{" + left + operator.getSymbol() + right + "}";
    }

    public <U> Expression<U> with(U left, U right) {
        return new DefaultExpression<>(left, operator, right);
    }

    public Expression<T> flipOperator() {
        return new DefaultExpression<>(left, operator.flip(), right);
    }

    public T requireLeft() { return Objects.requireNonNull(left); }

    public T getLeftOrDefault(T resultIfNone) { return left == null ? resultIfNone: left; }

    public Operator getOperator() {
        return operator;
    }

    public T requireRight() { return Objects.requireNonNull(right); }

    public T getRightOrDefault(T resultIfNone) { return right == null ? resultIfNone: right; }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DefaultExpression<?> that = (DefaultExpression<?>) o;
        return Objects.equals(left, that.left) && operator.equals(that.operator) && Objects
                .equals(right, that.right);
    }

    @Override public int hashCode() {
        return Objects.hash(left, operator, right);
    }

    public String getId() { return id; }

    @Override
    public String toString() {
        return getId();
    }
}
