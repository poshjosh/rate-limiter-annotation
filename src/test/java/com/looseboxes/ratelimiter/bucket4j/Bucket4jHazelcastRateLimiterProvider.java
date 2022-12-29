package com.looseboxes.ratelimiter.bucket4j;

import com.hazelcast.map.IMap;
import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.annotation.ResourceLimiterFromAnnotationFactory;
import com.looseboxes.ratelimiter.bandwidths.Bandwidth;
import com.looseboxes.ratelimiter.bucket4j.Bucket4JResourceLimiter;
import com.looseboxes.ratelimiter.bucket4j.Bucket4JResourceLimiterFactory;
import com.looseboxes.ratelimiter.bucket4j.ProxyManagerProvider;
import com.looseboxes.ratelimiter.cache.RateCache;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.hazelcast.Hazelcast;

import java.io.Serializable;

public class Bucket4jHazelcastRateLimiterProvider<K extends Serializable>{

    private static class ProxyManagerProviderImpl implements ProxyManagerProvider{
        @Override
        public <K extends Serializable> ProxyManager<K> getProxyManager(RateCache<K, ?> rateCache) {
            // It is possible to get a com.hazelcast.map.IMap via RateCache#unwrap(Class),
            // only if the RateCache is implemented over an com.hazelcast.map.IMap,
            // e.g using com.looseboxes.ratelimiter.cache.MapRateCache
            return Bucket4j.extension(Hazelcast.class).proxyManagerForMap(rateCache.unwrap(IMap.class));
        }
    }

    public ResourceLimiter<K> newInstance(IMap<K, GridBucketState> cache, Bandwidth... rates) {
        ProxyManager<K> proxyManager = Bucket4j.extension(Hazelcast.class).proxyManagerForMap(cache);
        return new Bucket4JResourceLimiter<>(proxyManager, rates);
    }

    public ResourceLimiter<K> newInstanceFromAnnotatedClasses(IMap<K, GridBucketState> cache, Class<?>... classes) {
        return ResourceLimiterFromAnnotationFactory.<K, GridBucketState>of()
                // TODO - Not really, NO LONGER MAINTAINED
                //.rateLimiterFactory(new Bucket4JResourceLimiterFactory<>(new ProxyManagerProviderImpl()))
                .rateLimiterConfig(ResourceLimiterConfig.<K, GridBucketState>builder().cache(RateCache.of(cache)).build())
                .create(classes);
    }
}
