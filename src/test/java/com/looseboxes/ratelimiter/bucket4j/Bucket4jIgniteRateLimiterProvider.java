package com.looseboxes.ratelimiter.bucket4j;

import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.ResourceLimiterConfig;
import com.looseboxes.ratelimiter.annotation.ResourceLimiterFromAnnotationFactory;
import com.looseboxes.ratelimiter.bandwidths.Bandwidth;
import com.looseboxes.ratelimiter.bucket4j.Bucket4JResourceLimiter;
import com.looseboxes.ratelimiter.bucket4j.Bucket4JResourceLimiterFactory;
import com.looseboxes.ratelimiter.bucket4j.ProxyManagerProvider;
import com.looseboxes.ratelimiter.cache.RateCache;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.ignite.Ignite;
import org.apache.ignite.IgniteCache;

import java.io.Serializable;

public class Bucket4jIgniteRateLimiterProvider<K extends Serializable>{

    private static class ProxyManagerProviderImpl implements ProxyManagerProvider{
        @Override
        public <K extends Serializable> ProxyManager<K> getProxyManager(RateCache<K, ?> rateCache) {
            // It is possible to get a org.apache.ignite.IgniteCache via RateCache#unwrap(Class),
            // only if the RateCache is implemented over an org.apache.ignite.IgniteCache,
            // e.g using com.looseboxes.ratelimiter.cache.JavaRateCache
            return Bucket4j.extension(Ignite.class).proxyManagerForCache(rateCache.unwrap(IgniteCache.class));
        }
    }

    public ResourceLimiter<K> newInstance(IgniteCache<K, GridBucketState> cache, Bandwidth... rates) {
        ProxyManager<K> proxyManager = Bucket4j.extension(Ignite.class).proxyManagerForCache(cache);
        return new Bucket4JResourceLimiter<>(proxyManager, rates);
    }

    public ResourceLimiter<K> newInstanceFromAnnotatedClasses(IgniteCache<K, GridBucketState> cache, Class<?>... classes) {
        return ResourceLimiterFromAnnotationFactory.<K, GridBucketState>of()
                // TODO - Not really, NO LONGER MAINTAINED
                //.rateLimiterFactory(new Bucket4JResourceLimiterFactory<>(new ProxyManagerProviderImpl()))
                .rateLimiterConfig(ResourceLimiterConfig.<K, GridBucketState>builder().cache(RateCache.of(cache)).build())
                .create(classes);
    }
}
