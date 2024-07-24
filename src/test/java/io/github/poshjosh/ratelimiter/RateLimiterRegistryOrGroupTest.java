package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterRegistryOrGroupTest {
    private static final String OR_RATE_GROUP = "or-rate-group";
    private static final int MIN = 1;
    private static final int MAX = 2;

    @Rate(MIN)
    @Rate(MAX)
    @RateGroup(id = OR_RATE_GROUP, operator = Operator.OR)
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
        static Method getRateLimitedMethod() {
            try {
                return RateLimitGroupClass2.class.getDeclaredMethod("method0");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void orGroupClass_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = givenRateLimiterRegistryHavingOrGroup();
        Class<?> clazz = RateLimitGroupClass1.class;
        assertTrue(limiterRegistry.getClassRateLimiter(clazz).tryAcquire(MIN));
        assertFalse(limiterRegistry.getClassRateLimiter(clazz).tryAcquire());
    }

    @Test
    void orGroupMethod_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = givenRateLimiterRegistryHavingOrGroup();
        Method method = RateLimitGroupClass2.getRateLimitedMethod();
        assertTrue(limiterRegistry.getMethodRateLimiter(method).tryAcquire(MIN));
        assertFalse(limiterRegistry.getMethodRateLimiter(method).tryAcquire());
    }

    @Test
    void orGroupName_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = givenRateLimiterRegistryHavingOrGroup();
        assertTrue(limiterRegistry.getRateLimiter(OR_RATE_GROUP).tryAcquire(MIN));
        assertFalse(limiterRegistry.getRateLimiter(OR_RATE_GROUP).tryAcquire());
    }

    @Test
    void orGroupAnnotation_shouldNotBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = givenRateLimiterRegistryHavingOrGroup();
        assertTrue(limiterRegistry.getClassRateLimiter(RateLimitGroup.class).tryAcquire(Integer.MAX_VALUE));
    }

    private RateLimiterRegistry<Object> givenRateLimiterRegistryHavingOrGroup() {
        // The classes should not be in order, as is expected in real situations
        return RateLimiterRegistries.of(RateLimitGroupClass1.class, RateLimitGroup.class, RateLimitGroupClass2.class);
    }
}
