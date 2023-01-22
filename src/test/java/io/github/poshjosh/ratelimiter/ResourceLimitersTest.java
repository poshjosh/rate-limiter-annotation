package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.node.Node;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class ResourceLimitersTest {

    static final String key = "one";

    @Rate(permits = 1, name = key)
    static class RateLimitedClass0{ }

    @Test
    void testRateLimitedClass() {
        ResourceLimiter<Object> limiter = buildRateLimiter(1, RateLimitedClass0.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    static class RateLimitedClass1{
        @Rate(permits = 1, name = key)
        void method_0() { }
    }

    @Test
    void testClassWithSingleRateLimitedMethod() {
        ResourceLimiter<Object> limiter = buildRateLimiter(2, RateLimitedClass1.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(permits = 1, name = "class")
    static class RateLimitedClass2{
        @Rate(permits = 1, name = "method")
        void method_0() { }
    }

    @Test
    void testRateLimitedClassWithSingleRateLimitedMethod() {
        ResourceLimiter<Object> limiter = buildRateLimiter(2, RateLimitedClass2.class);
        assertTrue(limiter.tryConsume("class"));
        assertFalse(limiter.tryConsume("class"));
    }

    @Rate(permits = 1, timeUnit = SECONDS)
    @Rate(permits = 3, timeUnit = SECONDS)
    @RateGroup(name = RateLimitedClass3.groupId, operator = Operator.OR)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @interface RateLimitedClass3Group{ }

    @RateLimitedClass3Group
    static class RateLimitedClass3{
        private static final String groupId = "rate-limited-class3";
    }

    @Test
    void testRateLimitedClassWithOrLimits() {
        ResourceLimiter<Object> limiter = buildRateLimiter(2, RateLimitedClass3.class);
        assertTrue(limiter.tryConsume(RateLimitedClass3.groupId));
        assertFalse(limiter.tryConsume(RateLimitedClass3.groupId));
    }

    @Rate(permits = 1, timeUnit = SECONDS)
    @Rate(permits = 3, timeUnit = SECONDS)
    @RateGroup(name = RateLimitedClass4.groupId, operator = Operator.AND)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @interface RateLimitedClass4Group {  }

    @RateLimitedClass4Group
    static class RateLimitedClass4{
        private static final String groupId = "rate-limited-class4";
    }

    @Test
    void testRateLimitedClassWithAndLimits() {
        ResourceLimiter<Object> limiter = buildRateLimiter(2, RateLimitedClass4.class);
        assertTrue(limiter.tryConsume(RateLimitedClass4.groupId));
        assertTrue(limiter.tryConsume(RateLimitedClass4.groupId));
        assertTrue(limiter.tryConsume(RateLimitedClass4.groupId));
        assertFalse(limiter.tryConsume(RateLimitedClass4.groupId));
    }

    @Rate(permits = 10, timeUnit = SECONDS)
    static class RateLimitedClass5{
        @Rate(permits = 10, timeUnit = SECONDS)
        void method_0() { }
    }

    @Test
    void testAndThen() {
        ResourceLimiter<Object> a = buildRateLimiter(2, RateLimitedClass5.class);
        ResourceLimiter<Object> b = ResourceLimiter.of(Bandwidth.bursty(1));
        ResourceLimiter<Object> c = a.andThen(b);
        final Object key = "one";
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
    void testGroupMetaAnnotation() {
        // The classes should be not be in order, as is expected in real situations
        ResourceLimiter<Object> limiter = buildRateLimiter(4,
                MyRateGroupMember0.class, MyRateGroup.class, MyRateGroupMember1.class);
        assertTrue(limiter.tryConsume(RATE_GROUP_NAME));
        assertTrue(limiter.tryConsume(RATE_GROUP_NAME));
        assertFalse(limiter.tryConsume(RATE_GROUP_NAME));
    }

    @Rate(1)
    @RateCondition("sys.memory.free<1")
    public class RateLimitedClass6{ }

    @Test
    void givenRateConditionFalse_shouldNotBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(1, RateLimitedClass6.class);
        assertTrue(limiter.tryConsume(key));
        assertTrue(limiter.tryConsume(key));
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed>PT0S")
    public class RateLimitedClass7{ }

    @Test
    void givenRateConditionTrue_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(1, RateLimitedClass7.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(1)
    @RateCondition(" sys.time.elapsed > PT0S ")
    public class RateLimitedClass8{ }

    @Test
    void givenRateConditionTrue_andHavingSpaces_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(1, RateLimitedClass8.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed!<=PT0S")
    public class RateLimitedClass9{ }

    @Test
    void givenRateConditionFalse_afterNegation_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(1, RateLimitedClass9.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    private ResourceLimiter<Object> buildRateLimiter(int expectedNodes, Class<?>... classes) {
        return buildRateLimiter(expectedNodes, new HashSet<>(Arrays.asList(classes)));
    }

    private ResourceLimiter<Object> buildRateLimiter(int expectedNodes, Set<Class<?>> classes) {

        Node<RateConfig> rootNode = RateProcessor.ofDefaults().processAll(classes);
        System.out.println(rootNode);

        assertEquals(expectedNodes, numberOfNodes(rootNode));

        return new ResourceLimiters<>(ResourceLimiters.MatcherProvider.ofDefaults(),
                ResourceLimiters.LimiterProvider.ofDefaults(), rootNode);
    }

    private int numberOfNodes(Node node) {
        final AtomicInteger count = new AtomicInteger();
        node.visitAll(currentNode -> count.incrementAndGet());
        return count.decrementAndGet(); // We subtract the root node
    }
}
