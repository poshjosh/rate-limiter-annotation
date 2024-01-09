package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.BandwidthState;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class ResourceLimiterAnnotationTest {

    @Rate(permits = 1, name = "resource-0")
    static class RateLimitedClass0{ }

    @Test
    void testRateLimitedClass() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass0.class);
        assertTrue(limiter.tryConsume("resource-0"));
        assertFalse(limiter.tryConsume("resource-0"));
    }

    static class RateLimitedClass1{
        @Rate(1)
        void hi() { }
    }

    @Test
    void testClassWithSingleRateLimitedMethod() throws Exception {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass1.class);
        final String resourceId = ElementId.of(RateLimitedClass1.class.getDeclaredMethod("hi"));
        assertTrue(limiter.tryConsume(resourceId));
        assertFalse(limiter.tryConsume(resourceId));
    }

    @Rate(permits = 1, name = "class")
    static class RateLimitedClass2{
        @Rate(permits = 1, name = "method")
        void method_0() { }
    }

    @Test
    void testRateLimitedClassWithSingleRateLimitedMethod() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass2.class);
        assertTrue(limiter.tryConsume("method"));
        assertFalse(limiter.tryConsume("method"));
    }

    @Rate(permits = 1, timeUnit = SECONDS)
    @Rate(permits = 3, timeUnit = SECONDS)
    @RateGroup(operator = Operator.OR)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @interface RateLimitedClass3Group{ }

    @RateLimitedClass3Group
    static class RateLimitedClass3{ }

    @Test
    void testRateLimitedClassWithOrLimits() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass3.class);
        final String id = ElementId.of(RateLimitedClass3.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(permits = 1, timeUnit = SECONDS)
    @Rate(permits = 3, timeUnit = SECONDS)
    @RateGroup(operator = Operator.AND)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @interface RateLimitedClass4Group {  }

    @RateLimitedClass4Group
    static class RateLimitedClass4{ }

    @Test
    void testRateLimitedClassWithAndLimits() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass4.class);
        final String id = ElementId.of(RateLimitedClass4.class);
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(permits = 10, timeUnit = SECONDS)
    static class RateLimitedClass5{
        @Rate(permits = 10, timeUnit = SECONDS)
        void method_0() { }
    }

    @Test
    void testAndThen() {
        final String key = "one";
        ResourceLimiter<Object> a = buildRateLimiter(RateLimitedClass5.class);
        ResourceLimiter<Object> b =
            ResourceLimiter.of(key, io.github.poshjosh.ratelimiter.model.Rate.ofSeconds(1));
        ResourceLimiter<Object> c = a.andThen(b);
        assertTrue(c.tryConsume(key));
        assertFalse(c.tryConsume(key));
    }

    private static final String RATE_GROUP_NAME = "my-rate-group";

    @MyRateGroup
    static class MyRateGroupMember0{
        void method0() {}
    }

    static class MyRateGroupMember1{
        @MyRateGroup
        void method0() {}
    }

    @Rate(1)
    @Rate(2)
    @RateGroup(name = RATE_GROUP_NAME, operator = Operator.AND)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    public @interface MyRateGroup { }

    @Test
    void testGroupMember_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = testGroupLimiter();
        final String id = ElementId.of(MyRateGroupMember0.class);
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Test
    void testGroupAnnotation_shouldNotBeRateLimited() {
        ResourceLimiter<Object> limiter = testGroupLimiter();
        final String id = ElementId.of(MyRateGroupMember0.class);
        assertTrue(limiter.tryConsume(RATE_GROUP_NAME));
        assertTrue(limiter.tryConsume(RATE_GROUP_NAME));
        assertTrue(limiter.tryConsume(RATE_GROUP_NAME));
    }

    private ResourceLimiter<Object> testGroupLimiter() {
        // The classes should not be in order, as is expected in real situations
        return buildRateLimiter(MyRateGroupMember0.class, MyRateGroup.class, MyRateGroupMember1.class);
    }

    @Rate(1)
    @RateCondition("jvm.memory.free<1")
    public class RateLimitedClass6{ }

    @Test
    void givenRateConditionFalse_shouldNotBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass6.class);
        final String id = ElementId.of(RateLimitedClass6.class);
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
    }

    @Rate(permits=1, when="jvm.memory.free<0")
    public class RateLimitedClass6b{ }

    @Test
    void givenRateWhenResolvesToFalse_shouldNotBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass6b.class);
        final String id = ElementId.of(RateLimitedClass6b.class);
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed>PT0S")
    public class RateLimitedClass7{ }

    @Test
    void givenRateConditionTrue_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass7.class);
        final String id = ElementId.of(RateLimitedClass7.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(permits=1, when="sys.time.elapsed>PT0S")
    public class RateLimitedClass7b{ }

    @Test
    void givenRateWhenResolvesToTrue_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass7b.class);
        final String id = ElementId.of(RateLimitedClass7b.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(1)
    @RateCondition(" sys.time.elapsed > PT0S ")
    public class RateLimitedClass8{ }

    @Test
    void givenRateConditionTrue_andHavingSpaces_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass8.class);
        final String id = ElementId.of(RateLimitedClass8.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(name = "resource-8b", permits=1, when=" sys.time.elapsed > PT0S ")
    public class RateLimitedClass8b{ }

    @Test
    void givenRateWhenResolvesToTrue_andHavingSpaces_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass8b.class);
        assertTrue(limiter.tryConsume("resource-8b"));
        assertFalse(limiter.tryConsume("resource-8b"));
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed!<=PT0S")
    public class RateLimitedClass9{ }

    @Test
    void givenRateConditionHavingNegationResolvesToTrue_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass9.class);
        final String id = ElementId.of(RateLimitedClass9.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    @Rate(permits=1, when="sys.time.elapsed!<=PT0S")
    public class RateLimitedClass9b{ }

    @Test
    void givenWhenHavingNegationResolvesToTrue_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass9b.class);
        final String id = ElementId.of(RateLimitedClass9b.class);
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    // This first Rate's condition will never evaluate to true,
    // as elapsed time will always be greater than zero.
    @Rate(permits=1, when="sys.time.elapsed<=PT0S")
    @Rate(permits=2, when="sys.time.elapsed>PT0S")
    public class RateLimitedClass10{ }

    @Test
    void givenNonComposedRates() {
        ResourceLimiter<Object> limiter = buildRateLimiter(RateLimitedClass10.class);
        final String id = ElementId.of(RateLimitedClass10.class);
        assertTrue(limiter.tryConsume(id));
        assertTrue(limiter.tryConsume(id));
        assertFalse(limiter.tryConsume(id));
    }

    private ResourceLimiter<Object> buildRateLimiter(Class<?>... classes) {
        return ResourceLimiter.of(classes);
    }
}
