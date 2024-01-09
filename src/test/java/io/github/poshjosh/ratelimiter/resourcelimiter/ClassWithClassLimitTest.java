package io.github.poshjosh.ratelimiter.resourcelimiter;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Rate(2)
class ClassWithClassLimitTest {

    @Test
    void isNotLimited() {
        final ResourceLimiter<String> limiter = getResourceLimiterForThisClass();
        final String key = getMethodId("isNotLimited");
        assertTrue(limiter.tryConsume(key, Integer.MAX_VALUE));
    }

    // TODO - Fix this failing test
    //@Test
    @Rate(Integer.MAX_VALUE)
    void isLimitedByClassGivenClassRateIsLower() {
        final ResourceLimiter<String> limiter = getResourceLimiterForThisClass();
        final String key = getMethodId("isLimitedByClassGivenClassRateIsLower");
        assertTrue(limiter.tryConsume(key));
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Test
    @Rate(1)
    void isLimitedByMethodGivenMethodRateIsLower() {
        final ResourceLimiter<String> limiter = getResourceLimiterForThisClass();
        final String key = getMethodId("isLimitedByMethodGivenMethodRateIsLower");
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(name = "named", permits = 10)
    static class ClassWithNamedRate { }
    @Test
    void isLimitedByName() {
        final ResourceLimiter<String> limiter = ResourceLimiter.of(ClassWithNamedRate.class);
        final String key = "named";
        assertTrue(limiter.tryConsume(key, 10));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(name="conditional", permits = 5, condition = "sys.time.elapsed>PT2S")
    static class ClassWithConditionalRate {

    }

    @Test
    void isLimitedConditionally() throws InterruptedException {
        final ResourceLimiter<String> limiter = ResourceLimiter.of(ClassWithConditionalRate.class);
        final String key = "conditional";
        // This consumption attempt should have returned false due to limit exceeded,
        // but we have a condition that must be met before rate limiting is applied
        assertTrue(limiter.tryConsume(key, Integer.MAX_VALUE));
        Thread.sleep(2000); // Time should match that of the condition specified above
        assertTrue(limiter.tryConsume(key, 5));
        assertFalse(limiter.tryConsume(key));
    }

    private ResourceLimiter<String> getResourceLimiterForThisClass() {
        return ResourceLimiter.of(this.getClass());
    }

    private String getMethodId(String methodName) {
        return ElementId.of(getMethod(methodName));
    }

    private Method getMethod(String name) {
        try {
            return this.getClass().getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
