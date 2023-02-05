package io.github.poshjosh.ratelimiter.expression;

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
    public static final Operator NOT_EQUALS = new Operator("!=", Type.COMPARISON);
    public static final Operator NOT_GREATER = new Operator("!>", Type.COMPARISON);
    public static final Operator NOT_GREATER_OR_EQUALS = new Operator("!>=", Type.COMPARISON);
    public static final Operator NOT_LESS = new Operator("!<", Type.COMPARISON);
    public static final Operator NOT_LESS_OR_EQUALS = new Operator("!<=", Type.COMPARISON);
    public static final Operator NOT_LIKE = new Operator("!%", Type.STRING);
    public static final Operator NOT_STARTS_WITH = new Operator("!^", Type.STRING);
    public static final Operator NOT_ENDS_WITH = new Operator("!$", Type.STRING);
    public static Operator[] values() {
        return new Operator[]{
                EQUALS, GREATER, GREATER_OR_EQUALS, LESS,
                LESS_OR_EQUALS, LIKE, STARTS_WITH, ENDS_WITH,
                NOT_EQUALS, NOT_GREATER, NOT_GREATER_OR_EQUALS, NOT_LESS,
                NOT_LESS_OR_EQUALS, NOT_LIKE, NOT_STARTS_WITH, NOT_ENDS_WITH
        };
    }
    public static Operator of(String symbol) {
        if (symbol.startsWith("!")) {
            return of(symbol.substring(1)).flip();
        }
        switch(symbol) {
            case "=": return EQUALS;
            case ">": return GREATER;
            case ">=": return GREATER_OR_EQUALS;
            case "<": return LESS;
            case "<=": return LESS_OR_EQUALS;
            case "%": return LIKE;
            case "^": return STARTS_WITH;
            case "$": return ENDS_WITH;
            default: throw Checks.notSupported(Operator.class, symbol);
        }
    }
    private final String symbol;
    private final Type type;
    private Operator(String symbol, Type type) {
        this.symbol = Checks.requireContent(symbol);
        this.type = Objects.requireNonNull(type);
    }
    public boolean isNegation() {
        return symbol.startsWith("!");
    }
    public Operator positive() {
        return !isNegation() ? this : flip();
    }
    public Operator negative() {
        return isNegation() ? this : flip();
    }
    public Operator flip() {
        return new Operator((isNegation() ? symbol.substring(1) : "!" + symbol), type);
    }
    public Type getType() {
        return type;
    }
    public String getSymbol() {
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Operator operator = (Operator) o;
        return symbol.equals(operator.symbol) && type == operator.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, type);
    }

    @Override
    public String toString() {
        return symbol;
    }
}
