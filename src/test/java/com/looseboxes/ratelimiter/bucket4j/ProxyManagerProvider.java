package com.looseboxes.ratelimiter.bucket4j;

import com.looseboxes.ratelimiter.cache.RateCache;
import io.github.bucket4j.grid.ProxyManager;

import java.io.Serializable;

@FunctionalInterface
public interface ProxyManagerProvider {
    <K extends Serializable> ProxyManager<K> getProxyManager(RateCache<K, ?> cache);
}
