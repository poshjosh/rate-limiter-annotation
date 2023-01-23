package io.github.poshjosh.ratelimiter.matcher;

import java.util.Objects;

public final class Operator {
    enum Type {COMPARISON, STRING}
    public static final Operator EQUALS = new Operator("=", Type.COMPARISON);
    public static final Operator GREATER = new Operator(">", Type.COMPARISON);
    public static final Operator GREATER_OR_EQUALS = new Operator(">=", Type.COMPARISON);
    public static final Operator LESS = new Operator("<", Type.COMPARISON);
    public static final Operator LESS_OR_EQUALS = new Operator("<=", Type.COMPARISON);
    public static final Operator LIKE = new Operator("%", Type.STRING);
    public static final Operator STARTS_WITH = new Operator("^", Type.STRING);
    public static final Operator ENDS_WITH = new Operator("$", Type.STRING);
    public static Operator of(String s) {
        if (s.startsWith("!")) {
            return of(s.substring(1)).flip();
        }
        switch(s) {
            case "=": return EQUALS;
            case ">": return GREATER;
            case ">=": return GREATER_OR_EQUALS;
            case "<": return LESS;
            case "<=": return LESS_OR_EQUALS;
            case "%": return LIKE;
            case "^": return STARTS_WITH;
            case "$": return ENDS_WITH;
            default: throw Checks.notSupported(Operator.class, s);
        }
    }
    private final String value;
    private final Type type;
    private Operator(String value, Type type) {
        this.value = Checks.requireContent(value);
        this.type = Objects.requireNonNull(type);
    }
    public boolean isNegation() {
        return value.startsWith("!");
    }
    public Operator positive() {
        return !isNegation() ? this : flip();
    }
    public Operator negative() {
        return isNegation() ? this : flip();
    }
    public Operator flip() {
        return new Operator((isNegation() ? value.substring(1) : "!" + value), type);
    }
    public Type getType() {
        return type;
    }
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Operator operator = (Operator) o;
        return value.equals(operator.value) && type == operator.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @Override
    public String toString() {
        return value;
    }
}
