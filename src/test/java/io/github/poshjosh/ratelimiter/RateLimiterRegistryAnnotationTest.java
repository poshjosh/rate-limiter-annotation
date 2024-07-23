package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.lang.reflect.Method;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterRegistryAnnotationTest {

    @Rate(permits = 1, id = "resource-0")
    static class RateLimitedClass { }

    @Test
    void testRateLimitedClass() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(RateLimitedClass.class);
        assertTrue(limiterRegistry.getRateLimiter("resource-0").tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter("resource-0").tryAcquire());
    }

    static class ClassWithRateLimitedMethod {
        @Rate(1)
        void hi() { }
        static Method getRateLimitedMethod() {
            try {
                return ClassWithRateLimitedMethod.class.getDeclaredMethod("hi");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void testClassWithSingleRateLimitedMethod() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithRateLimitedMethod.class);
        Object resourceId = ClassWithRateLimitedMethod.getRateLimitedMethod();
        assertTrue(limiterRegistry.getRateLimiter(resourceId).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(resourceId).tryAcquire());
    }

    static class ClassWithNoLimit {
        void hi() { }
    }

    @Test
    void isNotLimited() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithNoLimit.class);
        Object resourceId = ClassWithRateLimitedMethod.getRateLimitedMethod();
        assertTrue(limiterRegistry.getRateLimiter(resourceId).tryAcquire(Integer.MAX_VALUE));
    }

    @Rate(2)
    static class ClassRateHigherThanMethodRate {
        @Rate(1)
        void hi() { }
        static Method getRateLimitedMethod() {
            try {
                return ClassRateHigherThanMethodRate.class.getDeclaredMethod("hi");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
    @Test
    void isLimitedByMethodGivenMethodRateIsLower() {
        final RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassRateHigherThanMethodRate.class);
        final Object key = ClassRateHigherThanMethodRate.getRateLimitedMethod();
        assertTrue(limiterRegistry.getRateLimiter(key).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(key).tryAcquire());
    }

    @Rate(1)
    static class ClassRateLowerThanMethodRate {
        @Rate(2)
        void hi() { }
        static Method getRateLimitedMethod() {
            try {
                return ClassRateLowerThanMethodRate.class.getDeclaredMethod("hi");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void methodIsLimitedByClassGivenClassRateIsLower() throws NoSuchMethodException {
        final RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassRateLowerThanMethodRate.class);
        final Method key = ClassRateLowerThanMethodRate.class.getDeclaredMethod("hi");
        assertTrue(limiterRegistry.getRateLimiter(key).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(key).tryAcquire());
    }

    @Test
    void methodIdentifiedByIdIsLimitedByClassGivenClassRateIsLower() {
        final RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassRateLowerThanMethodRate.class);
        final Object key = ClassRateLowerThanMethodRate.getRateLimitedMethod();
        assertTrue(limiterRegistry.getRateLimiter(key).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(key).tryAcquire());
    }

    @Rate(1)
    @RateGroup
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @interface RateGroupWithLowRate { }

    @RateGroupWithLowRate
    static class ClassWithGroupRateLower1 {
        @Rate(2)
        void hi() { }
        static Method getRateLimitedMethod() {
            try {
                return ClassWithGroupRateLower1.class.getDeclaredMethod("hi");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @RateGroupWithLowRate
    static class ClassWithGroupRateLower2 {
        @Rate(2)
        void hi() { }
    }

    @Test
    void methodIsLimitedByGroupGivenGroupRateIsLower() throws NoSuchMethodException {
        final RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(
                ClassWithGroupRateLower1.class);
        final Method key = ClassWithGroupRateLower1.class.getDeclaredMethod("hi");
        assertTrue(limiterRegistry.getRateLimiter(key).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(key).tryAcquire());
    }

    @Test
    void methodIdentifiedByIdIsLimitedByGroupGivenGroupRateIsLower() {
        final RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(
                ClassWithGroupRateLower1.class);
        final Object key = ClassWithGroupRateLower1.getRateLimitedMethod();
        assertTrue(limiterRegistry.getRateLimiter(key).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(key).tryAcquire());
    }

    @Test
    void multipleMethodsAreLimitedByGroupGivenGroupRateIsLower() throws NoSuchMethodException {
        final RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(
                ClassWithGroupRateLower1.class, ClassWithGroupRateLower2.class);
        final Method key1 = ClassWithGroupRateLower1.class.getDeclaredMethod("hi");
        assertTrue(limiterRegistry.getRateLimiter(key1).tryAcquire());
        final Method key2 = ClassWithGroupRateLower2.class.getDeclaredMethod("hi");
        assertFalse(limiterRegistry.getRateLimiter(key2).tryAcquire());
    }

    @Rate(permits = 1, id = "class")
    static class RateLimitedClassWithNamedRateLimitedMethod {
        @Rate(permits = 1, id = "method")
        void method_0() { }
    }

    @Test
    void testRateLimitedClassWithNamedRateLimitedMethod() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(
                RateLimitedClassWithNamedRateLimitedMethod.class);
        assertTrue(limiterRegistry.getRateLimiter("method").tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter("method").tryAcquire());
    }

    @Rate(permits = 1, timeUnit = SECONDS)
    @Rate(permits = 3, timeUnit = SECONDS)
    @RateGroup(operator = Operator.OR)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @interface OrRateGroup { }

    @OrRateGroup
    static class ClassWithOrRateGroup { }

    @Test
    void testRateLimitedClassWithOrLimits() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithOrRateGroup.class);
        final Object id = ClassWithOrRateGroup.class;
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits = 1, timeUnit = SECONDS)
    @Rate(permits = 3, timeUnit = SECONDS)
    @RateGroup(operator = Operator.AND)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @interface AndRateGroup {  }

    @AndRateGroup
    static class ClassWithAndRateGroup { }

    @Test
    void testRateLimitedClassWithAndLimits() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithAndRateGroup.class);
        final Object id = ClassWithAndRateGroup.class;
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits = 10, timeUnit = SECONDS)
    static class ClassWithClassAndMethodLimits {
        @Rate(permits = 10, timeUnit = SECONDS)
        void method_0() { }
    }

    @Rate(1)
    @RateCondition("jvm.memory.free<1")
    static class ClassWithSeparateRateCondition { }

    @Test
    void givenRateConditionFalse_shouldNotBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithSeparateRateCondition.class);
        final Object id = (ClassWithSeparateRateCondition.class);
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits=1, when="jvm.memory.free<0")
    static class ClassWithWhenRateCondition { }

    @Test
    void givenRateWhenResolvesToFalse_shouldNotBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithWhenRateCondition.class);
        final Object id = (ClassWithWhenRateCondition.class);
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed>=PT0S")
    static class ClassWithRateConditionTrue { }

    @Test
    void givenRateConditionTrue_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithRateConditionTrue.class);
        final Object id = (ClassWithRateConditionTrue.class);
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits=1, when="sys.time.elapsed>=PT0S")
    static class ClassWithWhenRateConditionTrue { }

    @Test
    void givenRateWhenResolvesToTrue_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithWhenRateConditionTrue.class);
        final Object id = (ClassWithWhenRateConditionTrue.class);
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits = 1, condition = "sys.time.elapsed>=PT2S")
    static class ClassWithRateCondition { }

    @Test
    void isLimitedConditionally() throws InterruptedException {
        final RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithRateCondition.class);
        final Object key = (ClassWithRateCondition.class);
        // This consumption attempt should have returned false due to limit exceeded,
        // but we have a condition that must be met before rate limiting is applied
        assertTrue(limiterRegistry.getRateLimiter(key).tryAcquire(Integer.MAX_VALUE));
        Thread.sleep(2000); // Time should match that of the condition specified above
        assertTrue(limiterRegistry.getRateLimiter(key).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(key).tryAcquire());
    }

    @Rate(1)
    @RateCondition(" sys.time.elapsed >= PT0S ")
    static class ClassWithSeparateRateConditionSpaced { }

    @Test
    void givenRateConditionTrue_andHavingSpaces_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(
                ClassWithSeparateRateConditionSpaced.class);
        final Object id = (ClassWithSeparateRateConditionSpaced.class);
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(id).tryAcquire());
    }

    @Rate(id = "resource-8b", permits=1, when=" sys.time.elapsed >= PT0S ")
    static class ClassWithWhenRateConditionSpaced { }

    @Test
    void givenRateWhenResolvesToTrue_andHavingSpaces_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithWhenRateConditionSpaced.class);
        assertTrue(limiterRegistry.getRateLimiter("resource-8b").tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter("resource-8b").tryAcquire());
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed!<PT0S") // We have had 0 secs, which may cause !<= to fail
    static class ClassWithNegationSeparateRateCondition { }

    @Test
    void givenRateConditionHavingNegationResolvesToTrue_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithNegationSeparateRateCondition.class);
        final Object id = (ClassWithNegationSeparateRateCondition.class);
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits=1, when="sys.time.elapsed!<PT0S") // We have had 0 secs, which may cause !<= to fail
    static class ClassWithNegationWhenRateCondition { }

    @Test
    void givenWhenHavingNegationResolvesToTrue_shouldBeRateLimited() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithNegationWhenRateCondition.class);
        final Object id = (ClassWithNegationWhenRateCondition.class);
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(id).tryAcquire());
    }

    // This first Rate's condition will never evaluate to true,
    // as elapsed time will never be less than zero.
    @Rate(permits=1, when="sys.time.elapsed<PT0S")
    @Rate(permits=2, when="sys.time.elapsed>=PT0S")
    static class ClassWithNonConjunctedRates { }

    @Test
    void givenNonConjunctedRates() {
        RateLimiterRegistry<Object> limiterRegistry = newRateLimiterRegistry(ClassWithNonConjunctedRates.class);
        final Object id = (ClassWithNonConjunctedRates.class);
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertTrue(limiterRegistry.getRateLimiter(id).tryAcquire());
        assertFalse(limiterRegistry.getRateLimiter(id).tryAcquire());
    }

    private RateLimiterRegistry<Object> newRateLimiterRegistry(Class<?>... classes) {
        return RateLimiterRegistry.of(classes);
    }
}
