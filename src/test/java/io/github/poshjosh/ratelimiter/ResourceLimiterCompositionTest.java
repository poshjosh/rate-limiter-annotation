package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.AnnotationProcessor;
import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.annotation.Rate;
import io.github.poshjosh.ratelimiter.annotation.RateGroup;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class ResourceLimiterCompositionTest {

    final Object key = "one";

    @Rate(permits = 1, timeUnit = SECONDS)
    static class RateLimitedClass0{ }

    @Test
    void testRateLimitedClass() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(1, RateLimitedClass0.class);
        assertTrue(resourceLimiter.tryConsume(key));
        assertFalse(resourceLimiter.tryConsume(key));
    }

    static class RateLimitedClass1{
        @Rate(permits = 1, timeUnit = SECONDS)
        void rateLimitedClass1_method_0() { }
    }

    @Test
    void testClassWithSingleRateLimitedMethod() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(1, RateLimitedClass1.class);
        assertTrue(resourceLimiter.tryConsume(key));
        assertFalse(resourceLimiter.tryConsume(key));
    }

    @Rate(permits = 1, timeUnit = SECONDS)
    static class RateLimitedClass2{
        @Rate(permits = 1, timeUnit = SECONDS)
        void rateLimitedClass2_method_0() { }
    }

    @Test
    void testRateLimitedClassWithSingleRateLimitedMethod() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(2, RateLimitedClass2.class);
        assertTrue(resourceLimiter.tryConsume(key));
        assertFalse(resourceLimiter.tryConsume(key));
    }

    @RateGroup(operator = Operator.OR)
    @Rate(permits = 1, timeUnit = SECONDS)
    @Rate(permits = 3, timeUnit = SECONDS)
    static class RateLimitedClass3{ }

    @Test
    void testRateLimitedClassWithOrLimits() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(1, RateLimitedClass3.class);
        assertTrue(resourceLimiter.tryConsume(key));
        assertFalse(resourceLimiter.tryConsume(key));
    }

    @RateGroup(operator = Operator.AND)
    @Rate(permits = 1, timeUnit = SECONDS)
    @Rate(permits = 3, timeUnit = SECONDS)
    static class RateLimitedClass4{ }

    @Test
    void testRateLimitedClassWithAndLimits() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(1, RateLimitedClass4.class);
        assertTrue(resourceLimiter.tryConsume(key));
        assertTrue(resourceLimiter.tryConsume(key));
        assertTrue(resourceLimiter.tryConsume(key));
        assertFalse(resourceLimiter.tryConsume(key));
    }

    @Rate(permits = 10, timeUnit = SECONDS)
    static class RateLimitedClass5{
        @Rate(permits = 10, timeUnit = SECONDS)
        void rateLimitedClass2_method_0() { }
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

    static final String MY_RATE_GROUP_NAME = "my-rate-group";

    @Rate(1)
    @RateGroup(MY_RATE_GROUP_NAME)
    public @interface MyRateGroup { }

    @RateGroup(MY_RATE_GROUP_NAME)
    static class MyRateGroupMember0{
        void method0() {}
    }

    static class MyRateGroupMember1{
        @RateGroup(MY_RATE_GROUP_NAME)
        void method0() {}
    }

    @Test
    void testGroupMetaAnnotation() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(2,
                MyRateGroup.class, MyRateGroupMember0.class, MyRateGroupMember1.class);
        assertTrue(resourceLimiter.tryConsume(MY_RATE_GROUP_NAME));
        assertFalse(resourceLimiter.tryConsume(MY_RATE_GROUP_NAME));
    }

    private ResourceLimiter<Object> buildRateLimiter(int expectedNodes, Class<?>... classes) {
        return buildRateLimiter(expectedNodes, Arrays.asList(classes));
    }

    private ResourceLimiter<Object> buildRateLimiter(int expectedNodes, List<Class<?>> classes) {

        Node<RateConfig> rootNode = Node.of("root");

        AnnotationProcessor.ofDefaults().processAll(rootNode, classes);
        //System.out.println(NodeFormatter.indentedHeirarchy().format(rootNode));

        assertEquals(expectedNodes, numberOfNodes(rootNode));

        return ResourceLimiterComposition.ofAnnotations(rootNode);
    }

    private int numberOfNodes(Node node) {
        final AtomicInteger count = new AtomicInteger();
        node.visitAll(currentNode -> count.incrementAndGet());
        return count.decrementAndGet(); // We subtract the root node
    }
}
