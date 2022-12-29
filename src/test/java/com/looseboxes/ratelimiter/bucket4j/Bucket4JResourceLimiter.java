package com.looseboxes.ratelimiter.bucket4j;

import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.ResourceUsageListener;
import com.looseboxes.ratelimiter.bandwidths.Bandwidth;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.util.SleepingTicker;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.grid.ProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * ResourceLimiter implementation based on bucket4j
 * @see <a href="https://github.com/vladimir-bukhtoyarov/bucket4j/blob/6.4/doc-pages/jcache-usage.md">bucket4j jcache usage</a>
 * @param <K> The type of the key which the {@link Bucket4JResourceLimiter#tryConsume(Object)}} method accepts.
 */
public class Bucket4JResourceLimiter<K extends Serializable> implements ResourceLimiter<K> {

    private static final Logger LOG = LoggerFactory.getLogger(Bucket4JResourceLimiter.class);

    private final SleepingTicker ticker = SleepingTicker.zeroOffset();

    private final ProxyManager<K> buckets;
    private final Supplier<BucketConfiguration>[] configurationSuppliers;
    private final ResourceUsageListener resourceUsageListener;
    private final Bandwidths limits;

    public Bucket4JResourceLimiter(ProxyManager<K> proxyManager, Bandwidth... rates) {
        this(proxyManager, Bandwidths.of(rates));
    }

    public Bucket4JResourceLimiter(ProxyManager<K> proxyManager, Bandwidths rates) {
        this(proxyManager, BucketConfigurationProvider.simple(), ResourceUsageListener.NO_OP, rates);
    }

    public Bucket4JResourceLimiter(
            ProxyManager<K> proxyManager,
            BucketConfigurationProvider bucketConfigurationProvider,
            ResourceUsageListener resourceUsageListener,
            Bandwidths rates) {
        this.buckets = Objects.requireNonNull(proxyManager);
        this.limits = Objects.requireNonNull(rates);
        Bandwidth [] members = rates.getMembers();
        this.configurationSuppliers = new Supplier[members.length];
        for(int i = 0; i < members.length; i++) {
            BucketConfiguration configuration = bucketConfigurationProvider.getBucketConfiguration(members[i]);
            this.configurationSuppliers[i] = () -> configuration;
        }
        this.resourceUsageListener = Objects.requireNonNull(resourceUsageListener);
    }

    @Override
    public boolean tryConsume(Object context, K resourceId, int amount, long timeout, TimeUnit unit) {

        int failCount = 0;

        for (Supplier<BucketConfiguration> configurationSupplier : configurationSuppliers) {

            Bucket bucket = buckets.getProxy(resourceId, configurationSupplier);

            if (tryAcquire(bucket, amount, timeout, unit)) {
                continue;
            }

            ++failCount;
        }

        final boolean limitExceeded = limits.isLimitExceeded(failCount);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Limit exceeded: {}, for: {}, failures: {}/{}, limit: {}",
                    limitExceeded, resourceId, failCount, configurationSuppliers.length, limits);
        }

        resourceUsageListener.onConsumed(context, resourceId, amount, limits);

        if (!limitExceeded) {
            return true;
        }

        resourceUsageListener.onRejected(context, resourceId, amount, limits);

        return false;
    }

    private boolean tryAcquire(Bucket bucket, int permits, long timeout, TimeUnit unit) {
        EstimationProbe estimate = bucket.estimateAbilityToConsume(permits);
        if (!estimate.canBeConsumed()) {
            final long nanosToWait = estimate.getNanosToWaitForRefill();
            final long timeoutNanos = unit.toNanos(timeout);
            if (nanosToWait > timeoutNanos) {
                return false;
            }
            sleepNanosUninterruptibly(nanosToWait);
        }
        bucket.tryConsume(permits);
        return true;
    }

    private void sleepNanosUninterruptibly(long nanosToWait) {
        ticker.sleepMicrosUninterruptibly(TimeUnit.NANOSECONDS.toMicros(nanosToWait));
    }

    @Override
    public String toString() {
        return "Bucket4JResourceLimiter@" + Integer.toHexString(hashCode()) + limits;
    }
}
