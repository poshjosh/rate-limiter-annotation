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

class RateLimiterRegistryAndGroupTest {
    private static final String AND_RATE_GROUP = "and-rate-group";
    private static final int MIN = 1;
    private static final int MAX = 2;

    @Rate(MIN)
    @Rate(MAX)
    @RateGroup(id = AND_RATE_GROUP, operator = Operator.AND)
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
    void andGroupClass_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = givenLimiterRegistryHavingAndGroup();
        Class<?> clazz = RateLimitGroupClass1.class;
        assertTrue(limiterRegistry.getClassRateLimiter(clazz).tryAcquire(MAX));
        assertFalse(limiterRegistry.getClassRateLimiter(clazz).tryAcquire());
    }

    @Test
    void andGroupMethod_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = givenLimiterRegistryHavingAndGroup();
        Method method = RateLimitGroupClass2.getRateLimitedMethod();
        assertTrue(limiterRegistry.getMethodRateLimiter(method).tryAcquire(MAX));
        assertFalse(limiterRegistry.getMethodRateLimiter(method).tryAcquire());
    }

    @Test
    void andGroupName_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = givenLimiterRegistryHavingAndGroup();
        assertTrue(limiterRegistry.getRateLimiter(AND_RATE_GROUP).tryAcquire(MAX));
        assertFalse(limiterRegistry.getRateLimiter(AND_RATE_GROUP).tryAcquire());
    }

    @Test
    void andGroupAnnotation_shouldNotBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = givenLimiterRegistryHavingAndGroup();
        assertTrue(limiterRegistry.getClassRateLimiter(RateLimitGroup.class).tryAcquire(Integer.MAX_VALUE));
    }

    private RateLimiterRegistry<Object> givenLimiterRegistryHavingAndGroup() {
        // The classes should not be in order, as is expected in real situations
        return RateLimiterRegistries.of(RateLimitGroupClass1.class, RateLimitGroup.class, RateLimitGroupClass2.class);
    }
}
