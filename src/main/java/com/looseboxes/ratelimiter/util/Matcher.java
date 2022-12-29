package com.looseboxes.ratelimiter.util;

import java.util.Objects;

@FunctionalInterface
public interface Matcher<T, R> {

    Matcher<Object, Object> MATCH_NONE = target -> null;
    Matcher<Object, Object> IDENTITY = target -> target;

    @SuppressWarnings("unchecked")
    static <T, K> Matcher<T, K> matchNone() {
        return (Matcher<T, K>)MATCH_NONE;
    }

    @SuppressWarnings("unchecked")
    static <T, K> Matcher<T, K> identity() {
        return (Matcher<T, K>)IDENTITY;
    }

    default boolean matches(T target) {
        return matchOrNull(target) != null;
    }

    R matchOrNull(T target);

    /**
     * Returns a composed {@code Matcher} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Matcher} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default Matcher<T, R> andThen(Matcher<? super T, ? super R> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            R result = matchOrNull(t);
            // If there was no match, do not continue
            if(result == null) {
                return result;
            }
            return (R)after.matchOrNull(t);
        };
    }
}
