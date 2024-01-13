package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.util.LimiterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

final class DefaultRateLimiterFactory<K> implements RateLimiterFactory<K> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRateLimiterFactory.class);

    private static final class Collector<K> implements RateLimiterTree.RateLimiterConsumer<K> {
        private RateLimiter[] collection;
        @Override
        public void accept(String match, RateLimiter rateLimiter, LimiterContext<K> context,
                int index, int total) {
            if (collection == null) {
                collection = new RateLimiter[total];
            }
            collection[index] = rateLimiter;
        }
    }

    private final RateLimiterTree<K> rateLimiterTree;
    DefaultRateLimiterFactory(RateLimiterTree<K> rateLimiterTree) {
        this.rateLimiterTree = Objects.requireNonNull(rateLimiterTree);
    }

    @Override
    public Optional<RateLimiter> getRateLimiter(K key) {
        // TODO - We could have cached the result of this method with the key.
        // First check that performance is really improved.
        // Then ensure that the cache will not grow indefinitely.
        // Also ensure that this method is idempotent. i.e the same key
        // will always return the same RateLimiter. This might not be
        // the case if the Key is HttpServletRequest and the RateLimiter
        // is returned based on some request related condition. In that
        // case, 2 different HttpServletRequests could result to the
        // same RateLimiter.
        return Optional.ofNullable(buildRateLimiter(key, null));
    }

    private RateLimiter buildRateLimiter(K key, RateLimiter resultIfNone) {
        Collector<K> collector = new Collector<>();
        rateLimiterTree.visitRateLimiters(key, collector);
        RateLimiter [] limiters = collector.collection;
        LOG.trace("key = {}, limiters = {}", key, limiters);
        if (limiters.length == 0) {
            return resultIfNone;
        }
        if (limiters.length == 1) {
            return limiters[0];
        }
        return new RateLimiterComposite(limiters);
    }
}
