package io.github.poshjosh.ratelimiter.expression;

import java.util.Objects;

public final class Expression<T> {

    public static final Expression<Object> TRUE = new Expression<>(null, Operator.EQUALS, null);
    public static final Expression<Object> FALSE = TRUE.flipOperator();

    @SuppressWarnings("unchecked")
    public static <T> Expression<T> ofTrue() {
        return (Expression<T>)TRUE;
    }
    public @SuppressWarnings("unchecked")
    static <T> Expression<T> ofFalse() {
        return (Expression<T>)FALSE;
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
        this.left = left;
        this.operator = Objects.requireNonNull(operator);
        this.right = right; // Nullable
    }
    public <U> Expression<U> with(U left, U right) {
        return Expression.of(left, operator, right);
    }
    public Expression<T> flipOperator() {
        return Expression.of(left, operator.flip(), right);
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
        Expression<?> that = (Expression<?>) o;
        return Objects.equals(left, that.left) && operator.equals(that.operator) && Objects
                .equals(right, that.right);
    }

    @Override public int hashCode() {
        return Objects.hash(left, operator, right);
    }

    public String getId() {
        return left + operator.getSymbol() + right;
    }

    @Override
    public String toString() {
        return "{" + left + operator.getSymbol() + right + '}';
    }
}
