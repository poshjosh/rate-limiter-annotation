package io.github.poshjosh.ratelimiter.resourcelimiter;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassWithMethodLimitTest {

    @Test
    void isNotLimited() {
        final ResourceLimiter<String> limiter = getResourceLimiterForThisClass();
        final String key = getMethodId("isNotLimited");
        assertTrue(limiter.tryConsume(key, Integer.MAX_VALUE));
    }

    @Test
    @Rate(1)
    void isLimitedByMethod() {
        final ResourceLimiter<String> limiter = getResourceLimiterForThisClass();
        final String key = getMethodId("isLimitedByMethod");
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Test
    @Rate(name = "limited", permits = 1)
    void isLimitedByName() {
        final ResourceLimiter<String> limiter = getResourceLimiterForThisClass();
        final String key = "limited";
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Test
    @Rate(permits = 1, condition = "sys.time.elapsed>PT2S")
    void isLimitedConditionally() throws InterruptedException {
        final ResourceLimiter<String> limiter = getResourceLimiterForThisClass();
        final String key = getMethodId("isLimitedConditionally");
        // This consumption attempt should have returned false due to limit exceeded,
        // but we have a condition that must be met before rate limiting is applied
        assertTrue(limiter.tryConsume(key, Integer.MAX_VALUE));
        Thread.sleep(2000); // Time should match that of the condition specified above
        assertTrue(limiter.tryConsume(key));
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
