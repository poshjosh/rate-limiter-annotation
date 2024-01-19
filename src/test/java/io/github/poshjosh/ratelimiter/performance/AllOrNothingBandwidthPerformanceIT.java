package io.github.poshjosh.ratelimiter.performance;

import io.github.poshjosh.ratelimiter.bandwidths.BandwidthFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class AllOrNothingBandwidthPerformanceIT extends PerformanceIT {
    @BeforeAll
    static void beforeAll() {
        System.setProperty("bandwidth-factory-class", BandwidthFactory.AllOrNothing.class.getName());
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("bandwidth-factory-class");
    }
}
