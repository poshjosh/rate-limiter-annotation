package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.lang.reflect.Method;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterFactoryAnnotationTest {

    @Rate(permits = 1, name = "resource-0")
    static class RateLimitedClass { }

    @Test
    void testRateLimitedClass() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(RateLimitedClass.class);
        assertTrue(limiterFactory.getRateLimiter("resource-0").tryAcquire());
        assertFalse(limiterFactory.getRateLimiter("resource-0").tryAcquire());
    }

    static class ClassWithRateLimitedMethod {
        @Rate(1)
        void hi() { }
    }

    @Test
    void testClassWithSingleRateLimitedMethod() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithRateLimitedMethod.class);
        final String resourceId = ElementId.ofMethod(ClassWithRateLimitedMethod.class, "hi");
        assertTrue(limiterFactory.getRateLimiter(resourceId).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(resourceId).tryAcquire());
    }

    static class ClassWithNoLimit {
        void hi() { }
    }

    @Test
    void isNotLimited() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithNoLimit.class);
        final String resourceId = ElementId.ofMethod(ClassWithRateLimitedMethod.class, "hi");
        assertTrue(limiterFactory.getRateLimiter(resourceId).tryAcquire(Integer.MAX_VALUE));
    }

    @Rate(2)
    static class ClassRateHigherThanMethodRate {
        @Rate(1)
        void hi() { }
    }
    @Test
    void isLimitedByMethodGivenMethodRateIsLower() {
        final RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassRateHigherThanMethodRate.class);
        final String key = ElementId.ofMethod(ClassRateHigherThanMethodRate.class, "hi");
        assertTrue(limiterFactory.getRateLimiter(key).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(key).tryAcquire());
    }

    @Rate(1)
    static class ClassRateLowerThanMethodRate {
        @Rate(2)
        void hi() { }
    }

    @Test
    void methodIsLimitedByClassGivenClassRateIsLower() throws NoSuchMethodException {
        final RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassRateLowerThanMethodRate.class);
        final Method key = ClassRateLowerThanMethodRate.class.getDeclaredMethod("hi");
        assertTrue(limiterFactory.getRateLimiter(key).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(key).tryAcquire());
    }

    @Test
    void methodIdentifiedByIdIsLimitedByClassGivenClassRateIsLower() {
        final RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassRateLowerThanMethodRate.class);
        final String key = ElementId.ofMethod(ClassRateLowerThanMethodRate.class, "hi");
        assertTrue(limiterFactory.getRateLimiter(key).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(key).tryAcquire());
    }

    @Rate(permits = 1, name = "class")
    static class RateLimitedClassWithNamedRateLimitedMethod {
        @Rate(permits = 1, name = "method")
        void method_0() { }
    }

    @Test
    void testRateLimitedClassWithNamedRateLimitedMethod() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(
                RateLimitedClassWithNamedRateLimitedMethod.class);
        assertTrue(limiterFactory.getRateLimiter("method").tryAcquire());
        assertFalse(limiterFactory.getRateLimiter("method").tryAcquire());
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
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithOrRateGroup.class);
        final String id = ElementId.of(ClassWithOrRateGroup.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(id).tryAcquire());
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
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithAndRateGroup.class);
        final String id = ElementId.of(ClassWithAndRateGroup.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits = 10, timeUnit = SECONDS)
    static class ClassWithClassAndMethodLimits {
        @Rate(permits = 10, timeUnit = SECONDS)
        void method_0() { }
    }

    @Rate(1)
    @RateCondition("jvm.memory.free<1")
    public class ClassWithSeparateRateCondition { }

    @Test
    void givenRateConditionFalse_shouldNotBeRateLimited() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithSeparateRateCondition.class);
        final String id = ElementId.of(ClassWithSeparateRateCondition.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits=1, when="jvm.memory.free<0")
    public class ClassWithWhenRateCondition { }

    @Test
    void givenRateWhenResolvesToFalse_shouldNotBeRateLimited() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithWhenRateCondition.class);
        final String id = ElementId.of(ClassWithWhenRateCondition.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed>PT0S")
    public class ClassWithRateConditionTrue { }

    @Test
    void givenRateConditionTrue_shouldBeRateLimited() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithRateConditionTrue.class);
        final String id = ElementId.of(ClassWithRateConditionTrue.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits=1, when="sys.time.elapsed>PT0S")
    public class ClassWithWhenRateConditionTrue { }

    @Test
    void givenRateWhenResolvesToTrue_shouldBeRateLimited() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithWhenRateConditionTrue.class);
        final String id = ElementId.of(ClassWithWhenRateConditionTrue.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits = 1, condition = "sys.time.elapsed>PT2S")
    public class ClassWithRateCondition { }

    @Test
    void isLimitedConditionally() throws InterruptedException {
        final RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithRateCondition.class);
        final String key = ElementId.of(ClassWithRateCondition.class);
        // This consumption attempt should have returned false due to limit exceeded,
        // but we have a condition that must be met before rate limiting is applied
        assertTrue(limiterFactory.getRateLimiter(key).tryAcquire(Integer.MAX_VALUE));
        Thread.sleep(2000); // Time should match that of the condition specified above
        assertTrue(limiterFactory.getRateLimiter(key).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(key).tryAcquire());
    }

    @Rate(1)
    @RateCondition(" sys.time.elapsed > PT0S ")
    public class ClassWithSeprateRateConditionSpaced { }

    @Test
    void givenRateConditionTrue_andHavingSpaces_shouldBeRateLimited() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithSeprateRateConditionSpaced.class);
        final String id = ElementId.of(ClassWithSeprateRateConditionSpaced.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(id).tryAcquire());
    }

    @Rate(name = "resource-8b", permits=1, when=" sys.time.elapsed > PT0S ")
    public class ClassWithWhenRateConditionSpaced { }

    @Test
    void givenRateWhenResolvesToTrue_andHavingSpaces_shouldBeRateLimited() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithWhenRateConditionSpaced.class);
        assertTrue(limiterFactory.getRateLimiter("resource-8b").tryAcquire());
        assertFalse(limiterFactory.getRateLimiter("resource-8b").tryAcquire());
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed!<=PT0S")
    public class ClassWithNegationSeparateRateCondition { }

    @Test
    void givenRateConditionHavingNegationResolvesToTrue_shouldBeRateLimited() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithNegationSeparateRateCondition.class);
        final String id = ElementId.of(ClassWithNegationSeparateRateCondition.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(id).tryAcquire());
    }

    @Rate(permits=1, when="sys.time.elapsed!<=PT0S")
    public class ClassWithNegationWhenRateCondition { }

    @Test
    void givenWhenHavingNegationResolvesToTrue_shouldBeRateLimited() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithNegationWhenRateCondition.class);
        final String id = ElementId.of(ClassWithNegationWhenRateCondition.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(id).tryAcquire());
    }

    // This first Rate's condition will never evaluate to true,
    // as elapsed time will always be greater than zero.
    @Rate(permits=1, when="sys.time.elapsed<=PT0S")
    @Rate(permits=2, when="sys.time.elapsed>PT0S")
    public class ClassWithNonConjunctedRates { }

    @Test
    void givenNonConjunctedRates() {
        RateLimiterFactory<Object> limiterFactory = newRateLimiterFactory(ClassWithNonConjunctedRates.class);
        final String id = ElementId.of(ClassWithNonConjunctedRates.class);
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertTrue(limiterFactory.getRateLimiter(id).tryAcquire());
        assertFalse(limiterFactory.getRateLimiter(id).tryAcquire());
    }

    private RateLimiterFactory<Object> newRateLimiterFactory(Class<?>... classes) {
        return RateLimiterFactory.of(classes);
    }
}
