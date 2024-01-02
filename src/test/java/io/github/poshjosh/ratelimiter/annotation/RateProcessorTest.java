package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RateProcessorTest {

    @Rate(1)
    static class Resource1{}
    static class Resource2{}
    static class Resource3{}

    @Test
    void shouldNotProcessNonRateLimitedClasses() {

      List<Class<?>> classList = Arrays.asList(Resource1.class, Resource2.class, Resource3.class);

      Node<RateConfig> rootNode = RateProcessor.ofDefaults().processAll(new HashSet<>(classList));

      // The root node plus one rate limited class i.e Resource1
      assertEquals(2, rootNode.size());
    }
}