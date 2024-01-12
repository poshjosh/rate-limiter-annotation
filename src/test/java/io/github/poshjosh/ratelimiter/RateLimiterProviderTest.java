package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.model.Rates;
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
        LimiterContext<Object> config = getConfig();
        RateLimiter limiter = rateLimiterProvider.getRateLimiter("test-id", config);
        assertTrue(limiter.tryAcquire(1));
        assertFalse(limiter.tryAcquire(1));
    }

    @Test
    void getLimiters_givenNoLimitsDefined_shouldNotBeRateLimited() {
        LimiterContext<Object> config = getConfigThatHasNoLimits();
        RateLimiter limiter = rateLimiterProvider.getRateLimiter("test-id", config);
        // Just asserting that this has no limit
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
    }

    private LimiterContext<Object> getConfigThatHasNoLimits() {
        return getConfig(Rates.empty());
    }

    private LimiterContext<Object> getConfig() {
      return getConfig(Rates.of(Rate.ofSeconds(1)));
    }

    private LimiterContext<Object> getConfig(Rates rates) {
        Bandwidth[] bandwidths = rateToBandwidthConverter
                .convert("id", rates, ticker.elapsedMicros());
        return LimiterContext.of(RateConfig.of(rates), bandwidths,
                Matcher.matchNone(), Collections.emptyList(), ticker);
    }
}