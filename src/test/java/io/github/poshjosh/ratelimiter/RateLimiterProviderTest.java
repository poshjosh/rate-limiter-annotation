package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateSource;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.util.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterProviderTest {

    final RateToBandwidthConverter rateToBandwidthConverter = RateToBandwidthConverter.ofDefaults();
    final Ticker ticker = Ticker.ofDefaults();
    final RateLimiterProvider<Object, String> rateLimiterProvider = RateLimiterProvider.ofDefaults();

    @Test
    void getLimiters_shouldReturnValidRateLimiter() {
        LimiterConfig<Object> config = getConfig("test-node-name");
        RateLimiter limiter = rateLimiterProvider.getRateLimiter("test-id", config);
        assertTrue(limiter.tryAcquire(1));
        assertFalse(limiter.tryAcquire(1));
    }

    @Test
    void getLimiters_givenNoLimitsDefined_shouldNotBeRateLimited() {
        LimiterConfig<Object> config = getConfigThatHasNoLimits("test-node-name");
        RateLimiter limiter = rateLimiterProvider.getRateLimiter("test-id", config);
        // Just asserting that this has no limit
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
    }

    private LimiterConfig<Object> getConfigThatHasNoLimits(String name) {
        return getConfig(name, Rates.of());
    }

    private LimiterConfig<Object> getConfig(String name) {
      return getConfig(name, Rates.of(Rate.ofSeconds(1)));
    }

    private LimiterConfig<Object> getConfig(String name, Rates rates) {
        Bandwidth[] bandwidths = rateToBandwidthConverter.convert(name, rates, ticker.elapsedMicros());
        return LimiterConfig.of(RateSource.of(name), rates, bandwidths,
                Matcher.matchNone(), Collections.emptyList(), ticker);
    }
}