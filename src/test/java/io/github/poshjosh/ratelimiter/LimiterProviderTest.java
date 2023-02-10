package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LimiterProviderTest {

    final LimiterProvider<Object, String> limiterProvider = LimiterProvider.ofDefaults();

    @Test
    void getLimiters_shouldReturnValidRateLimiter() {
        LimiterConfig<Object> config = getConfig("test-node-name");
        RateLimiter limiter = limiterProvider.getOrCreateLimiter("test-id", config);
        assertTrue(limiter.tryAcquire(1));
        assertFalse(limiter.tryAcquire(1));
    }

    @Test
    void getLimiters_givenNoLimitsDefined_shouldNotBeRateLimited() {
        LimiterConfig<Object> config = getConfigThatHasNoLimits("test-node-name");
        RateLimiter limiter = limiterProvider.getOrCreateLimiter("test-id", config);
        // Just asserting that this has no limit
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
    }

    private LimiterConfig<Object> getConfigThatHasNoLimits(String name) {
        return LimiterConfig.of(createNodeThatHasNoLimits(name));
    }

    private LimiterConfig<Object> getConfig(String name) {
        return LimiterConfig.of(createNode(name));
    }

    private Node createNode(String nodeName) {
      Rates rates = Rates.of(Rate.ofSeconds(1));
      RateConfig rateConfig = RateConfig.of(rates);
      return Node.ofDefaultParent(nodeName, rateConfig);
    }

    private Node createNodeThatHasNoLimits(String nodeName) {
        Rates rates = Rates.of();
        RateConfig rateConfig = RateConfig.of(rates);
        return Node.ofDefaultParent(nodeName, rateConfig);
    }
}