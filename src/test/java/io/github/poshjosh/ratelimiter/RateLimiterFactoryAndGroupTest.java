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

class RateLimiterFactoryAndGroupTest {
    private static final String AND_RATE_GROUP = "and-rate-group";
    private static final int MIN = 1;
    private static final int MAX = 2;

    @Rate(MIN)
    @Rate(MAX)
    @RateGroup(name = AND_RATE_GROUP, operator = Operator.AND)
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
    void andGroupMembers_shouldBeRateLimited(Class<?> clazz) {
        RateLimiterFactory<Object> limiterFactory = givenLimiterFactoryHavingAndGroup();
        final String id = ElementId.of(clazz);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire(MAX));
        assertFalse(limiterFactory.getRateLimiter(id).tryAcquire());
    }

    @Test
    void andGroupName_shouldBeRateLimited() {
        RateLimiterFactory<Object> limiterFactory = givenLimiterFactoryHavingAndGroup();
        assertTrue(limiterFactory.getRateLimiter(AND_RATE_GROUP).tryAcquire(MAX));
        assertFalse(limiterFactory.getRateLimiter(AND_RATE_GROUP).tryAcquire());
    }

    @Test
    void andGroupAnnotation_shouldNotBeRateLimited() {
        RateLimiterFactory<Object> limiterFactory = givenLimiterFactoryHavingAndGroup();
        String id = ElementId.of(RateLimitGroup.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire(Integer.MAX_VALUE));
    }

    private RateLimiterFactory<Object> givenLimiterFactoryHavingAndGroup() {
        // The classes should not be in order, as is expected in real situations
        return RateLimiterFactory.of(RateLimitGroupClass1.class, RateLimitGroup.class, RateLimitGroupClass2.class);
    }
}
