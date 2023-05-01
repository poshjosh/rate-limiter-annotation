package io.github.poshjosh.ratelimiter.util;

import java.util.Objects;

@FunctionalInterface
public interface Matcher<I> {

    String NO_MATCH = "";

    Matcher<Object> MATCH_NONE = new Matcher<Object>() {
        @Override public String match(Object target) { return Matcher.NO_MATCH; }
        @Override public String toString() { return Matcher.class.getSimpleName() + "$MATCH_NONE"; }
    };

    @SuppressWarnings("unchecked")
    static <T> Matcher<T> matchNone() {
        return (Matcher<T>)MATCH_NONE;
    }

    static String composeResults(String first, String second) {
        return first + '_' + second;
    }
    static boolean isMatch(String matchResult) { return matchResult.length() > 0; }

    default boolean matches(I input) { return match(input).length() > 0; }

    /**
     * Match the input. Return a match, or empty text, if there is no match.
     * @param input The input to match
     * @return A matching string, or empty text, if none.
     */
    String match(I input);

    /**
     * Returns a composed {@code Matcher} that returns a composed match result
     * only if both matchers succeed.
     *
     * <p>Compose a {@code Matcher} that performs, in sequence, this operation followed by the
     * {@code after} operation. If performing either operation throws an exception, it is relayed
     * to the caller of the composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed. Likewise, if this operation does not
     * match, the {@code after} operation will not be performed.
     *
     * @param after the after operation to perform
     * @return a composed {@code Matcher}
     * @throws NullPointerException if {@code after} is null
     */
    default Matcher<I> andThen(Matcher<? super I> after) {
        Objects.requireNonNull(after);
        return (I t) -> {
            final String result = match(t);
            // If there was no match, do not continue
            if(!Matcher.isMatch(result)) {
                return result;
            }
            final String afterResult = after.match(t);
            if(!Matcher.isMatch(afterResult)) {
                return afterResult;
            }
            return composeResults(result, afterResult);
        };
    }
}
