package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.bandwidths.Bandwidth;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.cache.RateCache;

class ResourceLimiterWithSingletonCacheTest extends AbstractResourceLimiterTest {

    public ResourceLimiterWithSingletonCacheTest() {
        super(BandwidthFactory.AllOrNothing.class, true);
    }

    @Override
    public ResourceLimiter<String> getRateLimiter(Bandwidth... limits) {
        ResourceLimiterConfig<String, ?> config =
                ResourceLimiterConfig.<String, Object>builder().cache(RateCache.singleton()).build();
        return ResourceLimiter.<String>of(config, Bandwidths.of(limits));
    }
}
