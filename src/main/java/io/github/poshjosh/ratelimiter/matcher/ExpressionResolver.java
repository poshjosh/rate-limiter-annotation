package io.github.poshjosh.ratelimiter.matcher;

import java.time.LocalDateTime;

/**
 * Resolves expressions of format [LHS OPERATOR RHS]
 *
 * Supported operators are:
 *
 * <pre>
 * =  equals
 * >  greater
 * >= greater or equals
 * <  less
 * <= less or equals
 * ^  starts with
 * $  ends with
 * %  contains
 * !  negates other operators (e.g !=, !>, !$ etc)
 * </pre>
 */
public interface ExpressionResolver<T> {

    static ExpressionResolver<Long> ofLong() { return new LongExpressionResolver(); }
    static ExpressionResolver<Double> ofDecimal() { return new DecimalExpressionResolver(); }
    static ExpressionResolver<String> ofString() {
        return new StringExpressionResolver();
    }
    static ExpressionResolver<LocalDateTime> ofDateTime() { return new DateTimeExpressionResolver(); }

    boolean resolve(Expression<T> expression);

    boolean isSupported(Operator operator);
}
