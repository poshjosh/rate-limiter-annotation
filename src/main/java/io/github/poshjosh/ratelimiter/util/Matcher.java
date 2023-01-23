package io.github.poshjosh.ratelimiter.util;

import java.util.Objects;

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

    default boolean matches(I target) {
        return matchOrNull(target) != null;
    }

    O matchOrNull(I target);

  /**
   * Returns a composed {@code Matcher} that returns the result of the first match,
   * only if both matchers succeed, otherwise it return's <code>null</code>.
   *
   * <p>Compose a {@code Matcher} that performs, in sequence, this operation followed by the {@code
   * after} operation. If performing either operation throws an exception, it is relayed to the
   * caller of the composed operation. If performing this operation throws an exception, the {@code
   * after} operation will not be performed.
   *
   * @param after the after operation to perform
   * @return a composed {@code Matcher} that the result of the first match, only if both matchers
   * succeed otherwise it return's <code>null</code>.
   * @throws NullPointerException if {@code lhs} or {@code rhs} is null
   */
  default Matcher<I, O> andThen(Matcher<? super I, ? super O> after) {
        Objects.requireNonNull(after);
        return (I t) -> {
            final O result = matchOrNull(t);
            // If there was no match, do not continue
            if(result == null) {
                return null;
            }
            return after.matchOrNull(t) == null ? null : result; // Result of first match
        };
    }
}
