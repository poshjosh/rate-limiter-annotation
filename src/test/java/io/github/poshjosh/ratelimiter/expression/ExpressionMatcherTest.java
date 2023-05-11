package io.github.poshjosh.ratelimiter.expression;

import io.github.poshjosh.ratelimiter.util.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionMatcherTest {

    @Test void matchNone_shouldNotMatchValidExpression() {
        Matcher matcher = ExpressionMatcher.matchNone().matcher("sys.time.elapsed>PT1S").orElse(null);
        assertNotNull(matcher);
        assertFalse(matcher.matches(0));
        assertFalse(matcher.matches(1000));
        assertFalse(matcher.matches(System.currentTimeMillis()));
    }

    @Test void matchNone_shouldNotMatchInvalidExpression() {
        Matcher matcher = ExpressionMatcher.matchNone().matcher("1=").orElse(null);
        assertNotNull(matcher);
        assertFalse(matcher.matches(0));
        assertFalse(matcher.matches(1000));
        assertFalse(matcher.matches(System.currentTimeMillis()));
    }

    @Test void ofDefault() {
    }

    @Test void any() {
    }

    @Test void ofSystemMemory() {
    }

    @Test void ofSystemTime() {
    }

    @Test void ofSystemTimeElapsed() {
        Matcher matcher = ExpressionMatcher.ofSystemTimeElapsed()
                .matcher("sys.time.elapsed>PT1H").orElse(null);
        assertNotNull(matcher);
        assertFalse(matcher.matches(System.currentTimeMillis()));
    }

    @Test void ofSystemTimeElapsed_compositeOr() {
        Matcher matcher = ExpressionMatcher.ofSystemTimeElapsed()
                .matcher("sys.time.elapsed>=PT0S|sys.time.elapsed<PT1M").orElse(null);
        assertNotNull(matcher);
        assertTrue(matcher.matches(System.currentTimeMillis()));
    }

    @Test void ofSystemTimeElapsed_compositeAnd() {
        Matcher matcher = ExpressionMatcher.ofSystemTimeElapsed()
                .matcher("sys.time.elapsed>=PT0S&sys.time.elapsed<PT1M").orElse(null);
        assertNotNull(matcher);
        assertTrue(matcher.matches(System.currentTimeMillis()));
    }

    @Test void ofSystemProperty() {
        String name0 = UUID.randomUUID().toString() + "0";
        String name1 = UUID.randomUUID().toString() + "1";
        Matcher matcher = ExpressionMatcher.ofSystemProperty()
                .matcher("sys.property={user.name=" + name0 +
                        "}|sys.property={user.name=" + name1 + "}").orElse(null);
        assertNotNull(matcher);
    }

    @Test void ofSystemEnvironment() {
    }

    @Test void ofJvmThread_matchingId() {
        final long currentThreadId = Thread.currentThread().getId();
        Matcher matcher = ExpressionMatcher.ofJvmThread()
                .matcher("jvm.thread.current.id=" + currentThreadId).orElse(null);
        assertNotNull(matcher);
        assertTrue(matcher.matches(currentThreadId));
    }

    @Test void ofJvmThread_notMatchingId() {
        Matcher matcher = ExpressionMatcher.ofJvmThread()
                .matcher("jvm.thread.current.id=111").orElse(null);
        assertNotNull(matcher);
        assertFalse(matcher.matches(222));
    }

    @Test void of() {
    }

    @Test void testOf() {
    }

    @Test void match() {
    }

    @Test void matcher() {
    }

    @Test void isSupported() {
    }

    @Test void testMatcher() {
    }
}