package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.util.Rate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateToBandwidthConverterTest {

    RateToBandwidthConverter uut = RateToBandwidthConverter.ofDefaults();

    @Test
    void convert() {
        final int permits = 1;
        Bandwidth bandwidth = uut.convert(getRate(permits));
        assertTrue(bandwidth.canAcquire(0, 0));
        bandwidth.reserveAndGetWaitLength(permits, 0);
        assertFalse(bandwidth.canAcquire(0, 0));
    }

    public Rate getRate(long permits) {
        return Rate.ofSeconds(permits);
    }
}