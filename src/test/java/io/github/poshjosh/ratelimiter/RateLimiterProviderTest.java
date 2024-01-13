package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.store.BandwidthsStore;
import io.github.poshjosh.ratelimiter.util.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterProviderTest {

    // TODO - Mock these, but first make sure each has its own unit tests
    final RateToBandwidthConverter rateToBandwidthConverter = RateToBandwidthConverter.ofDefaults();
    final BandwidthsStore<String> bandwidthsStore = BandwidthsStore.ofDefaults();
    final Ticker ticker = Ticker.ofDefaults();
    final RateLimiterProvider<String> rateLimiterProvider = RateLimiterProvider.of(
            rateToBandwidthConverter, bandwidthsStore, ticker
    );

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