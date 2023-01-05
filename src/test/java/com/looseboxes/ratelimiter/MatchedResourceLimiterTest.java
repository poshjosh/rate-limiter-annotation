package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.RateConfig;
import com.looseboxes.ratelimiter.annotations.Rate;
import com.looseboxes.ratelimiter.annotations.RateGroup;
import com.looseboxes.ratelimiter.bandwidths.Bandwidth;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Operator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class MatchedResourceLimiterTest {

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

    private ResourceLimiter<Object> buildRateLimiter(int expectedNodes, Class<?>... classes) {

        Node<RateConfig> rootNode = Node.of("root");

        AnnotationProcessor.ofDefaults().processAll(rootNode, classes);
        //System.out.println(NodeFormatter.indentedHeirarchy().format(rootNode));

        assertEquals(expectedNodes, numberOfNodes(rootNode));

        MatchedResourceLimiter.MatcherProvider<Object> matcherProvider =
                node -> key -> key + "--" + node.getName();

        MatchedResourceLimiter.LimiterProvider limiterProvider = this::getOrCreateLimiter;

        return MatchedResourceLimiter.ofAnnotations(matcherProvider, limiterProvider, rootNode);
    }

    private int numberOfNodes(Node node) {
        final AtomicInteger count = new AtomicInteger();
        node.visitAll(currentNode -> count.incrementAndGet());
        return count.decrementAndGet(); // We subtract the root node
    }

    private Map<String, ResourceLimiter<Object>> nameToLimiter = new HashMap<>();
    private ResourceLimiter<Object> getOrCreateLimiter(Node<RateConfig> node) {
        return nameToLimiter.computeIfAbsent(node.getName(), k -> createLimiter(node));
    }

    private ResourceLimiter<Object> createLimiter(Node<RateConfig> node) {
        return node.getValueOptional()
                .map(RateConfig::getValue)
                .map(rates -> RateToBandwidthConverter.ofDefaults().convert(rates))
                .map(ResourceLimiter::of)
                .orElse(ResourceLimiter.NO_OP);
    }
}
