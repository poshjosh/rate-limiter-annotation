package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceLimiterOrGroupTest {
    private static final String OR_RATE_GROUP = "or-rate-group";
    private static final int MIN = 1;
    private static final int MAX = 2;

    @Rate(MIN)
    @Rate(MAX)
    @RateGroup(name = OR_RATE_GROUP, operator = Operator.OR)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    public @interface RateLimitGroup { }

    @RateLimitGroup
    static class RateLimitGroupClass1 {
        void method0() {}
    }

    static class RateLimitGroupClass2 {
        @RateLimitGroup
        void method0() {}
    }

    @ParameterizedTest
    @ValueSource(classes = { RateLimitGroupClass1.class, RateLimitGroupClass2.class})
    void orGroupMembers_shouldBeRateLimited(Class<?> clazz) {
        ResourceLimiter<Object> limiter = andGroupMultiClassLimiter();
        final String id = ElementId.of(clazz);
        assertTrue(limiter.tryConsume(id, MIN));
        assertFalse(limiter.tryConsume(id));
    }

    @Test
    void orGroupName_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = andGroupMultiClassLimiter();
        assertTrue(limiter.tryConsume(OR_RATE_GROUP, MIN));
        assertFalse(limiter.tryConsume(OR_RATE_GROUP));
    }

    @Test
    void orGroupAnnotation_shouldNotBeRateLimited() {
        ResourceLimiter<Object> limiter = andGroupMultiClassLimiter();
        String id = ElementId.of(RateLimitGroup.class);
        assertTrue(limiter.tryConsume(id, Integer.MAX_VALUE));
    }

    private ResourceLimiter<Object> andGroupMultiClassLimiter() {
        // The classes should not be in order, as is expected in real situations
        return ResourceLimiter.of(RateLimitGroupClass1.class, RateLimitGroup.class, RateLimitGroupClass2.class);
    }
}
