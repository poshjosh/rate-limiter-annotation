package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.annotation.ResourceLimiterFromAnnotationFactory;
import com.looseboxes.ratelimiter.annotations.RateLimit;
import com.looseboxes.ratelimiter.annotations.RateLimitGroup;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Operator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class PatternMatchingResourceLimiterTest {

    final Object key = "one";

    @RateLimit(permits = 1, duration = 1, timeUnit = SECONDS)
    static class RateLimitedClass0{ }

    @Test
    void testRateLimitedClass() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(1, RateLimitedClass0.class);
        assertTrue(resourceLimiter.tryConsume(key));
        assertFalse(resourceLimiter.tryConsume(key));
    }

    static class RateLimitedClass1{
        @RateLimit(permits = 1, duration = 1, timeUnit = SECONDS)
        void rateLimitedClass1_method_0() { }
    }

    @Test
    void testClassWithSingleRateLimitedMethod() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(1, RateLimitedClass1.class);
        assertTrue(resourceLimiter.tryConsume(key));
        assertFalse(resourceLimiter.tryConsume(key));
    }

    @RateLimit(permits = 1, duration = 1, timeUnit = SECONDS)
    static class RateLimitedClass2{
        @RateLimit(permits = 1, duration = 1, timeUnit = SECONDS)
        void rateLimitedClass2_method_0() { }
    }

    @Test
    void testRateLimitedClassWithSingleRateLimitedMethod() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(2, RateLimitedClass2.class);
        assertTrue(resourceLimiter.tryConsume(key));
        assertFalse(resourceLimiter.tryConsume(key));
    }

    @RateLimitGroup(operator = Operator.OR)
    @RateLimit(permits = 1, duration = 1, timeUnit = SECONDS)
    @RateLimit(permits = 3, duration = 1, timeUnit = SECONDS)
    static class RateLimitedClass3{ }

    @Test
    void testRateLimitedClassWithOrLimits() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(1, RateLimitedClass3.class);
        assertTrue(resourceLimiter.tryConsume(key));
        assertFalse(resourceLimiter.tryConsume(key));
    }

    @RateLimitGroup(operator = Operator.AND)
    @RateLimit(permits = 1, duration = 1, timeUnit = SECONDS)
    @RateLimit(permits = 3, duration = 1, timeUnit = SECONDS)
    static class RateLimitedClass4{ }

    @Test
    void testRateLimitedClassWithAndLimits() {
        ResourceLimiter<Object> resourceLimiter = buildRateLimiter(1, RateLimitedClass4.class);
        assertTrue(resourceLimiter.tryConsume(key));
        assertTrue(resourceLimiter.tryConsume(key));
        assertTrue(resourceLimiter.tryConsume(key));
        assertFalse(resourceLimiter.tryConsume(key));
    }

    private ResourceLimiter<Object> buildRateLimiter(int expectedNodes, Class<?>... classes) {

        Node<NodeValue<ResourceLimiter<Object>>> rateLimiterRootNode =
                ResourceLimiterFromAnnotationFactory.ofDefaults().createNode(classes);
        //System.out.println(NodeFormatters.indentedHeirarchy().format(rateLimiterRootNode));

        assertEquals(expectedNodes, numberOfNodes(rateLimiterRootNode));

        PatternMatchingResourceLimiter.MatcherProvider<ResourceLimiter<Object>, Object> matcherProvider =
                node -> key -> node.getName();

        PatternMatchingResourceLimiter.LimiterProvider<ResourceLimiter<Object>> limiterProvider =
                node -> node.getValueOptional().map(NodeValue::getValue).orElse(ResourceLimiter.NO_OP);

        boolean firstMatchOnly = false; // false for annotations, true for properties
        return new PatternMatchingResourceLimiter<>(
                matcherProvider, limiterProvider, rateLimiterRootNode, firstMatchOnly);
    }

    private int numberOfNodes(Node node) {
        final AtomicInteger count = new AtomicInteger();
        node.visitAll(currentNode -> count.incrementAndGet());
        return count.decrementAndGet(); // We subtract the root node
    }
}
