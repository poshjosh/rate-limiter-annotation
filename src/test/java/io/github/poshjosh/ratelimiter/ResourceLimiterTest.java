package io.github.poshjosh.ratelimiter;

class ResourceLimiterTest extends AbstractResourceLimiterTest {
    ResourceLimiterTest() {
        super(BandwidthFactory.AllOrNothing.class, true);
    }
}