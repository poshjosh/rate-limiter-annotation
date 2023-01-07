package io.github.poshjosh.ratelimiter.utiil;

import io.github.poshjosh.ratelimiter.util.Matcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatcherTest {

    private final String key = "key";
    private final String resultOnSuccess = "Success";
    private final String resultOnFailure = null;
    private final Matcher match = (k) -> resultOnSuccess;
    private final Matcher noMatch = (k) -> resultOnFailure;

    @Test
    void matchThenNomatchShouldMatch() {
        testAndThen(match, match, resultOnSuccess);
    }

    @Test
    void matchThenNomatchShouldNotMatch() {
        testAndThen(match, noMatch, resultOnFailure);
    }

    @Test
    void nomatchThenMatchShouldNotMatch() {
        testAndThen(noMatch, match, resultOnFailure);
    }

    @Test
    void nomatchThenNomatchShouldNotMatch() {
        testAndThen(noMatch, noMatch, resultOnFailure);
    }

    private void testAndThen(Matcher lhs, Matcher rhs, Object expected) {
        final Matcher composed = lhs.andThen(rhs);
        final Object actual = composed.matchOrNull(key);
        assertEquals(expected, actual);
    }
}