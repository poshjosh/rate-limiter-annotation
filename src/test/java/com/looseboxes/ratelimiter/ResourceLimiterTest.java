package com.looseboxes.ratelimiter;

class ResourceLimiterTest extends AbstractResourceLimiterTest {
    ResourceLimiterTest() {
        super(BandwidthFactory.AllOrNothing.class, false);
    }
}