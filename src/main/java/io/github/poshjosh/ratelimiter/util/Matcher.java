package io.github.poshjosh.ratelimiter.util;

import java.util.Objects;
import java.util.function.BinaryOperator;

@FunctionalInterface
public interface Matcher<I, O> {

    Matcher<Object, Object> MATCH_NONE = new Matcher<Object, Object>() {
        @Override public Object matchOrNull(Object target) { return null; }
        @Override public String toString() { return Matcher.class.getSimpleName() + "$MATCH_NONE"; }
    };

    @SuppressWarnings("unchecked")
    static <T, K> Matcher<T, K> matchNone() {
        return (Matcher<T, K>)MATCH_NONE;
    }

    /**
     * Calls {@code first.andThen(second)} using a result composer that adds the 2 string results.
     * @see #andThen(Matcher, BinaryOperator)
     */
    static <T> Matcher<T, String> compose(Matcher<T, String> first, Matcher<T, String> second) {
        BinaryOperator<String> resultComposer = (s0, s1) -> s0 + "_" + s1;
        return first.andThen(second, resultComposer);
    }

    default boolean matches(I target) {
        return matchOrNull(target) != null;
    }

    O matchOrNull(I target);



    /**
     * Returns a composed {@code Matcher} that returns a composed match result
     * only if both matchers succeed, otherwise it return's <code>null</code>.
     *
     * <p>Compose a {@code Matcher} that performs, in sequence, this operation followed by the {@code
     * after} operation. If performing either operation throws an exception, it is relayed to the
     * caller of the composed operation. If performing this operation throws an exception, the {@code
     * after} operation will not be performed.
     *
     * @param after the after operation to perform
     * @param resultComposer Used to compose a result
     * @return a composed {@code Matcher}
     * @throws NullPointerException if {@code after} or {@code resultComposer} is null
     */
    default Matcher<I, O> andThen(Matcher<? super I, ? super O> after, BinaryOperator<O> resultComposer) {
        Objects.requireNonNull(after);
        return (I t) -> {
            final O result = matchOrNull(t);
            // If there was no match, do not continue
            if(result == null) {
                return null;
            }
            final O afterResult = (O)after.matchOrNull(t);
            return afterResult == null ? null : resultComposer.apply(result, afterResult);
        };
    }
}
