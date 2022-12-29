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
import io.github.bucket4j.grid.jcache.JCache;

import javax.cache.Cache;
import java.io.Serializable;

public class Bucket4jJCacheRateLimiterProvider<K extends Serializable>{

    private static class ProxyManagerProviderImpl implements ProxyManagerProvider{
        @Override
        public <K extends Serializable> ProxyManager<K> getProxyManager(RateCache<K, ?> rateCache) {
            // It is possible to get a javax.cache.Cache via RateCache#unwrap(Class),
            // only if the RateCache is implemented over an javax.cache.Cache,
            // e.g using com.looseboxes.ratelimiter.cache.JavaRateCache
            return Bucket4j.extension(JCache.class).proxyManagerForCache(rateCache.unwrap(Cache.class));
        }
    }

    public ResourceLimiter<K> newInstance(Cache<K, GridBucketState> cache, Bandwidth... rates) {
        ProxyManager<K> proxyManager = Bucket4j.extension(JCache.class).proxyManagerForCache(cache);
        return new Bucket4JResourceLimiter<>(proxyManager, rates);
    }

    public ResourceLimiter<K> newInstanceFromAnnotatedClasses(Cache<K, GridBucketState> cache, Class<?>... classes) {
        return ResourceLimiterFromAnnotationFactory.<K, GridBucketState>of()
                // TODO - Not really, NO LONGER MAINTAINED
                //.rateLimiterFactory(new Bucket4JResourceLimiterFactory<>(new ProxyManagerProviderImpl()))
                .rateLimiterConfig(ResourceLimiterConfig.<K, GridBucketState>builder().cache(RateCache.of(cache)).build())
                .create(classes);
    }
}
