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

class ResourceLimiterAnnotationTest {

    @Rate(permits = 1, name = "resource-0")
    static class RateLimitedClass { }

    @Test
    void testRateLimitedClass() {
        ResourceLimiter<Object> limiter = newResourceLimiter(RateLimitedClass.class);
        assertTrue(limiter.tryConsume("resource-0"));
        assertFalse(limiter.tryConsume("resource-0"));
    }

    static class ClassWithRateLimitedMethod {
        @Rate(1)
        void hi() { }
    }

    @Test
    void testClassWithSingleRateLimitedMethod() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithRateLimitedMethod.class);
        final String resourceId = ElementId.ofMethod(ClassWithRateLimitedMethod.class, "hi");
        assertTrue(limiter.tryConsume(resourceId));
        assertFalse(limiter.tryConsume(resourceId));
    }

    static class ClassWithNoLimit {
        void hi() { }
    }

    @Test
    void isNotLimited() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithNoLimit.class);
        final String resourceId = ElementId.ofMethod(ClassWithRateLimitedMethod.class, "hi");
        assertTrue(limiter.tryConsume(resourceId, Integer.MAX_VALUE));
    }

    @Rate(2)
    static class ClassRateHigherThanMethodRate {
        @Rate(1)
        void hi() { }
    }
    @Test
    void isLimitedByMethodGivenMethodRateIsLower() {
        final ResourceLimiter<Object> limiter = newResourceLimiter(ClassRateHigherThanMethodRate.class);
        final String key = ElementId.ofMethod(ClassRateHigherThanMethodRate.class, "hi");
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(1)
    static class ClassRateLowerThanMethodRate {
        @Rate(2)
        void hi() { }
    }

    @Test
    void methodIsLimitedByClassGivenClassRateIsLower() throws NoSuchMethodException {
        final ResourceLimiter<Object> limiter = newResourceLimiter(ClassRateLowerThanMethodRate.class);
        final Method key = ClassRateLowerThanMethodRate.class.getDeclaredMethod("hi");
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Test
    void methodIdentifiedByIdIsLimitedByClassGivenClassRateIsLower() {
        final ResourceLimiter<Object> limiter = newResourceLimiter(ClassRateLowerThanMethodRate.class);
        final String key = ElementId.ofMethod(ClassRateLowerThanMethodRate.class, "hi");
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(permits = 1, name = "class")
    static class RateLimitedClassWithNamedRateLimitedMethod {
        @Rate(permits = 1, name = "method")
        void method_0() { }
    }

    @Test
    void testRateLimitedClassWithNamedRateLimitedMethod() {
        ResourceLimiter<Object> limiter = newResourceLimiter(
                RateLimitedClassWithNamedRateLimitedMethod.class);
        assertTrue(limiter.tryConsume("method"));
        assertFalse(limiter.tryConsume("method"));
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
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithOrRateGroup.class);
        final String id = ElementId.of(ClassWithOrRateGroup.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
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
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithAndRateGroup.class);
        final String id = ElementId.of(ClassWithAndRateGroup.class);
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(permits = 10, timeUnit = SECONDS)
    static class ClassWithClassAndMethodLimits {
        @Rate(permits = 10, timeUnit = SECONDS)
        void method_0() { }
    }

    @Test
    void testAndThen() {
        final String key = "one";
        ResourceLimiter<Object> a = newResourceLimiter(ClassWithClassAndMethodLimits.class);
        ResourceLimiter<Object> b =
            ResourceLimiter.of(key, io.github.poshjosh.ratelimiter.model.Rate.ofSeconds(1));
        ResourceLimiter<Object> c = a.andThen(b);
        assertTrue(c.tryConsume(key));
        assertFalse(c.tryConsume(key));
    }

    private static final String RATE_GROUP_NAME = "my-rate-group";

    @Rate(1)
    @Rate(2)
    @RateGroup(name = RATE_GROUP_NAME, operator = Operator.AND)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    public @interface MultiClassRateGroup { }

    @MultiClassRateGroup
    static class MultiClassRateGroupMember1 {
        void method0() {}
    }

    static class MultiClassRateGroupMember2 {
        @MultiClassRateGroup
        void method0() {}
    }

    @Test
    void testGroupMember_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = newMultiClassLimiter();
        final String id = ElementId.of(MultiClassRateGroupMember1.class);
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Test
    void testGroupAnnotation_shouldNotBeRateLimited() {
        ResourceLimiter<Object> limiter = newMultiClassLimiter();
        final String id = ElementId.of(MultiClassRateGroupMember1.class);
        assertTrue(limiter.tryConsume(RATE_GROUP_NAME));
        assertTrue(limiter.tryConsume(RATE_GROUP_NAME));
        assertTrue(limiter.tryConsume(RATE_GROUP_NAME));
    }

    private ResourceLimiter<Object> newMultiClassLimiter() {
        // The classes should not be in order, as is expected in real situations
        return newResourceLimiter(MultiClassRateGroupMember1.class, MultiClassRateGroup.class, MultiClassRateGroupMember2.class);
    }

    @Rate(1)
    @RateCondition("jvm.memory.free<1")
    public class ClassWithSeparateRateCondition { }

    @Test
    void givenRateConditionFalse_shouldNotBeRateLimited() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithSeparateRateCondition.class);
        final String id = ElementId.of(ClassWithSeparateRateCondition.class);
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
    }

    @Rate(permits=1, when="jvm.memory.free<0")
    public class ClassWithWhenRateCondition { }

    @Test
    void givenRateWhenResolvesToFalse_shouldNotBeRateLimited() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithWhenRateCondition.class);
        final String id = ElementId.of(ClassWithWhenRateCondition.class);
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed>PT0S")
    public class ClassWithRateConditionTrue { }

    @Test
    void givenRateConditionTrue_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithRateConditionTrue.class);
        final String id = ElementId.of(ClassWithRateConditionTrue.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(permits=1, when="sys.time.elapsed>PT0S")
    public class ClassWithWhenRateConditionTrue { }

    @Test
    void givenRateWhenResolvesToTrue_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithWhenRateConditionTrue.class);
        final String id = ElementId.of(ClassWithWhenRateConditionTrue.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(permits = 1, condition = "sys.time.elapsed>PT2S")
    public class ClassWithRateCondition { }

    @Test
    void isLimitedConditionally() throws InterruptedException {
        final ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithRateCondition.class);
        final String key = ElementId.of(ClassWithRateCondition.class);
        // This consumption attempt should have returned false due to limit exceeded,
        // but we have a condition that must be met before rate limiting is applied
        assertTrue(limiter.tryConsume(key, Integer.MAX_VALUE));
        Thread.sleep(2000); // Time should match that of the condition specified above
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(1)
    @RateCondition(" sys.time.elapsed > PT0S ")
    public class ClassWithSeprateRateConditionSpaced { }

    @Test
    void givenRateConditionTrue_andHavingSpaces_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithSeprateRateConditionSpaced.class);
        final String id = ElementId.of(ClassWithSeprateRateConditionSpaced.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(name = "resource-8b", permits=1, when=" sys.time.elapsed > PT0S ")
    public class ClassWithWhenRateConditionSpaced { }

    @Test
    void givenRateWhenResolvesToTrue_andHavingSpaces_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithWhenRateConditionSpaced.class);
        assertTrue(limiter.tryConsume("resource-8b"));
        assertFalse(limiter.tryConsume("resource-8b"));
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed!<=PT0S")
    public class ClassWithNegationSeparateRateCondition { }

    @Test
    void givenRateConditionHavingNegationResolvesToTrue_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithNegationSeparateRateCondition.class);
        final String id = ElementId.of(ClassWithNegationSeparateRateCondition.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(permits=1, when="sys.time.elapsed!<=PT0S")
    public class ClassWithNegationWhenRateCondition { }

    @Test
    void givenWhenHavingNegationResolvesToTrue_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithNegationWhenRateCondition.class);
        final String id = ElementId.of(ClassWithNegationWhenRateCondition.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    // This first Rate's condition will never evaluate to true,
    // as elapsed time will always be greater than zero.
    @Rate(permits=1, when="sys.time.elapsed<=PT0S")
    @Rate(permits=2, when="sys.time.elapsed>PT0S")
    public class ClassWithComposedRates { }

    @Test
    void givenNonComposedRates() {
        ResourceLimiter<Object> limiter = newResourceLimiter(ClassWithComposedRates.class);
        final String id = ElementId.of(ClassWithComposedRates.class);
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    private ResourceLimiter<Object> newResourceLimiter(Class<?>... classes) {
        return ResourceLimiter.of(classes);
    }
}
