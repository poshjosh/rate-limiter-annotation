package io.github.poshjosh.ratelimiter.matcher;

import java.util.Locale;

final class SystemMemoryExpressionParser<S> implements ExpressionParser<S, Long> {
    
    private static final long MEMORY_AVAILABLE_AT_STARTUP = MemoryUtil.availableMemory();

    public static final String MEMORY_AVAILABLE = "sys.memory.available";
    public static final String MEMORY_FREE = "sys.memory.free";
    public static final String MEMORY_MAX = "sys.memory.max";
    public static final String MEMORY_TOTAL = "sys.memory.total";
    public static final String MEMORY_USED = "sys.memory.used";

    // b has to come before all others, because all others end with b
    private static final String [] SUFFIXES = {"yb", "zb", "eb", "pb", "tb", "gb", "mb", "kb", "b"};

    SystemMemoryExpressionParser() { }

    @Override
    public boolean isSupported(Expression<String> expression) {
        final String lhs = expression.getLeft();
        switch (lhs) {
            case MEMORY_AVAILABLE:
            case MEMORY_FREE:
            case MEMORY_MAX:
            case MEMORY_TOTAL:
            case MEMORY_USED:
                return Operator.OperatorType.COMPARISON.equals(expression.getOperator().getType());
            default:
                return false;
        }
    }

    @Override
    public Expression<Long> parse(S source, Expression<String> expression) {
        final String lhs = expression.getLeft();
        switch (lhs) {
            case MEMORY_AVAILABLE:
                return expression.with(MemoryUtil.availableMemory(), right(expression));
            case MEMORY_FREE:
                return expression.with(Runtime.getRuntime().freeMemory(), right(expression));
            case MEMORY_MAX:
                return expression.with(Runtime.getRuntime().maxMemory(), right(expression));
            case MEMORY_TOTAL:
                return expression.with(Runtime.getRuntime().totalMemory(), right(expression));
            case MEMORY_USED:
                return expression.with(MemoryUtil.usedMemory(MEMORY_AVAILABLE_AT_STARTUP), right(expression));
            default:
                throw Checks.notSupported(this, lhs);
        }
    }

    private Long right(Expression<String> expression) {
        // This .replace('_', '\u0000') did not yield the desired result in some cases
        final String rhs = expression.getRight().replace("_", "").toLowerCase(Locale.ROOT);
        for (int i = 0; i < SUFFIXES.length; i++) {
            final int factor = SUFFIXES.length - i - 1;
            if (rhs.endsWith(SUFFIXES[i])) {
                final String numStr = rhs.substring(0, rhs.length() - SUFFIXES[i].length());
                return Long.parseLong(numStr) * ((long)Math.pow(1000, factor));
            }
        }
        return Long.parseLong(rhs);
    }
}
