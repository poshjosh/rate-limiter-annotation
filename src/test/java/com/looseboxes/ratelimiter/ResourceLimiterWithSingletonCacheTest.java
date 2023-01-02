package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.bandwidths.Bandwidth;
import com.looseboxes.ratelimiter.cache.RateCache;

class ResourceLimiterWithSingletonCacheTest extends AbstractResourceLimiterTest {

    public ResourceLimiterWithSingletonCacheTest() {
        super(BandwidthFactory.AllOrNothing.class, true);
    }

    @Override
    public ResourceLimiter<String> getRateLimiter(Bandwidth... limits) {
        return ResourceLimiter.<String>of(limits).cache(RateCache.singleton());
    }
}
