package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.AnnotationProcessor;
import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class ResourceLimiterCompositionTest {

    final Object key = "one";

    @Rate(1)
    static class RateLimitedClass0{ }

    @Test
    void testRateLimitedClass() {
        ResourceLimiter<Object> limiter = buildRateLimiter(2, RateLimitedClass0.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    static class RateLimitedClass1{
        @Rate(permits = 1, timeUnit = SECONDS)
        void rateLimitedClass1_method_0() { }
    }

    @Test
    void testClassWithSingleRateLimitedMethod() {
        ResourceLimiter<Object> limiter = buildRateLimiter(3, RateLimitedClass1.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(permits = 1, timeUnit = SECONDS)
    static class RateLimitedClass2{
        @Rate(permits = 1, timeUnit = SECONDS)
        void rateLimitedClass2_method_0() { }
    }

    @Test
    void testRateLimitedClassWithSingleRateLimitedMethod() {
        ResourceLimiter<Object> limiter = buildRateLimiter(4, RateLimitedClass2.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @RateGroup(name = RateLimitedClass3.id, operator = Operator.OR)
    @Rate(permits = 1, timeUnit = SECONDS)
    @Rate(permits = 3, timeUnit = SECONDS)
    static class RateLimitedClass3{
        private static final String id = "rate-limited-class3";
    }

    @Test
    void testRateLimitedClassWithOrLimits() {
        ResourceLimiter<Object> limiter = buildRateLimiter(2, RateLimitedClass3.class);
        assertTrue(limiter.tryConsume(RateLimitedClass3.id));
        assertFalse(limiter.tryConsume(RateLimitedClass3.id));
    }

    @RateGroup(name = RateLimitedClass4.id, operator = Operator.AND)
    @Rate(permits = 1, timeUnit = SECONDS)
    @Rate(permits = 3, timeUnit = SECONDS)
    static class RateLimitedClass4{
        private static final String id = "rate-limited-class4";
    }

    @Test
    void testRateLimitedClassWithAndLimits() {
        ResourceLimiter<Object> limiter = buildRateLimiter(2, RateLimitedClass4.class);
        assertTrue(limiter.tryConsume(RateLimitedClass4.id));
        assertTrue(limiter.tryConsume(RateLimitedClass4.id));
        assertTrue(limiter.tryConsume(RateLimitedClass4.id));
        assertFalse(limiter.tryConsume(RateLimitedClass4.id));
    }

    @Rate(permits = 10, timeUnit = SECONDS)
    static class RateLimitedClass5{
        @Rate(permits = 10, timeUnit = SECONDS)
        void rateLimitedClass2_method_0() { }
    }

    @Test
    void testAndThen() {
        ResourceLimiter<Object> a = buildRateLimiter(4, RateLimitedClass5.class);
        ResourceLimiter<Object> b = ResourceLimiter.of(Bandwidth.bursty(1));
        ResourceLimiter<Object> c = a.andThen(b);
        final Object key = "one";
        assertTrue(c.tryConsume(key));
        assertFalse(c.tryConsume(key));
    }

    private static final String RATE_GROUP_NAME = "my-rate-group";

    @RateGroup(RATE_GROUP_NAME)
    static class MyRateGroupMember0{
        void method0() {}
    }

    static class MyRateGroupMember1{
        @RateGroup(RATE_GROUP_NAME)
        void method0() {}
    }

    @Rate(1)
    @Rate(2)
    @RateGroup(value = RATE_GROUP_NAME, operator = Operator.AND)
    public @interface MyRateGroup { }

    @Test
    void testGroupMetaAnnotation() {
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
        ResourceLimiter<Object> limiter = buildRateLimiter(2, RateLimitedClass6.class);
        assertTrue(limiter.tryConsume(key));
        assertTrue(limiter.tryConsume(key));
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed>PT0S")
    public class RateLimitedClass7{ }

    @Test
    void givenRateConditionTrue_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(2, RateLimitedClass7.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(1)
    @RateCondition(" sys.time.elapsed > PT0S ")
    public class RateLimitedClass8{ }

    @Test
    void givenRateConditionTrue_andHavingSpaces_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(2, RateLimitedClass8.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    @Rate(1)
    @RateCondition("sys.time.elapsed!<=PT0S")
    public class RateLimitedClass9{ }

    @Test
    void givenRateConditionFalse_afterNegation_shouldBeRateLimited() {
        ResourceLimiter<Object> limiter = buildRateLimiter(2,RateLimitedClass9.class);
        assertTrue(limiter.tryConsume(key));
        assertFalse(limiter.tryConsume(key));
    }

    private ResourceLimiter<Object> buildRateLimiter(int expectedNodes, Class<?>... classes) {
        return buildRateLimiter(expectedNodes, Arrays.asList(classes));
    }

    private ResourceLimiter<Object> buildRateLimiter(int expectedNodes, List<Class<?>> classes) {

        Node<RateConfig> rootNode = Node.of("root");

        rootNode = AnnotationProcessor.ofDefaults().processAll(rootNode, classes);
        System.out.println(rootNode);

        //assertEquals(expectedNodes, numberOfNodes(rootNode));

        return ResourceLimiterComposition.ofAnnotations(rootNode);
    }

    private int numberOfNodes(Node node) {
        final AtomicInteger count = new AtomicInteger();
        node.visitAll(currentNode -> count.incrementAndGet());
        return count.decrementAndGet(); // We subtract the root node
    }
}
