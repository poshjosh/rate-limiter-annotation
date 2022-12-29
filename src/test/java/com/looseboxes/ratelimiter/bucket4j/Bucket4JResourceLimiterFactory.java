package com.looseboxes.ratelimiter.bucket4j;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.cache.RateCache;
import io.github.bucket4j.grid.ProxyManager;

import java.io.Serializable;
import java.util.Objects;

public class Bucket4JResourceLimiterFactory<K extends Serializable> {//implements ResourceLimiterFactory<K> {

    private final ProxyManagerProvider proxyManagerProvider;
    private final BucketConfigurationProvider bucketConfigurationProvider;

    public Bucket4JResourceLimiterFactory(ProxyManagerProvider proxyManagerProvider) {
        this(proxyManagerProvider, BucketConfigurationProvider.simple());
    }

    public Bucket4JResourceLimiterFactory(
            ProxyManagerProvider proxyManagerProvider,
            BucketConfigurationProvider bucketConfigurationProvider) {
        this.proxyManagerProvider = Objects.requireNonNull(proxyManagerProvider);
        this.bucketConfigurationProvider = Objects.requireNonNull(bucketConfigurationProvider);
    }

    //@Override
    public ResourceLimiter<K> createNew(ResourceLimiterConfig<K, ?> resourceLimiterConfig, Bandwidths rates) {

        RateCache<K, ?> rateCache = resourceLimiterConfig.getCache();

        ProxyManager<K> proxyManager = proxyManagerProvider.getProxyManager(rateCache);

        ResourceUsageListener resourceUsageListener = resourceLimiterConfig.getUsageListener();

        return new Bucket4JResourceLimiter<>(proxyManager, bucketConfigurationProvider, resourceUsageListener, rates);
    }
}
