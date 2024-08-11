package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterProviderTest {

    // TODO - Mock this
    final RateLimiterProvider rateLimiterProvider = RateLimiterProviders.ofDefaults();

    @Test
    void getLimiters_shouldReturnValidRateLimiter() {
        Rates rates = getRates();
        RateLimiter limiter = rateLimiterProvider.getRateLimiter("test-id", rates);
        assertTrue(limiter.tryAcquire(1));
        assertFalse(limiter.tryAcquire(1));
    }

    @Test
    void getLimiters_givenNoLimitsDefined_shouldNotBeRateLimited() {
        Rates rates = getRatesThatHasNoLimits();
        RateLimiter limiter = rateLimiterProvider.getRateLimiter("test-id", rates);
        // Just asserting that this has no limit
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
        assertTrue(limiter.tryAcquire(Integer.MAX_VALUE));
    }

    private Rates getRatesThatHasNoLimits() {
        return Rates.empty();
    }

    private Rates getRates() {
      return Rates.of(Rate.ofSeconds(1));
    }
}