package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

final class DefaultRateLimiterFactory<K> implements RateLimiterFactory<K> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRateLimiterFactory.class);

    private static final class Collector implements RateLimiterTree.RateLimiterConsumer {
        private RateLimiter single;
        private List<RateLimiter> collection;
        @Override
        public void accept(String match, RateLimiter rateLimiter,
                RateConfig config, int index, int max) {
            if (index == -1) {
                single = rateLimiter;
                return;
            }
            if (collection == null) {
                collection = new ArrayList<>(max);
            }
            collection.add(rateLimiter);
        }
    }

    private final RateLimiterTree<K> rateLimiterTree;
    DefaultRateLimiterFactory(RateLimiterTree<K> rateLimiterTree) {
        this.rateLimiterTree = Objects.requireNonNull(rateLimiterTree);
    }

    @Override
    public RateLimiter getRateLimiterOrDefault(K key, RateLimiter resultIfNone) {
        // TODO - We could have cached the result of this method with the key.
        // First check that performance is really improved.
        // Then ensure that the cache will not grow indefinitely.
        // Also ensure that this method is idempotent. i.e the same key
        // will always return the same RateLimiter. This might not be
        // the case if the Key is HttpServletRequest and the RateLimiter
        // is returned based on some request related condition. In that
        // case, 2 different HttpServletRequests could result to the
        // same RateLimiter.
        return buildRateLimiter(key, RateLimiter.NO_LIMIT);
    }

    private RateLimiter buildRateLimiter(K key, RateLimiter resultIfNone) {
        Collector collector = new Collector();
        rateLimiterTree.visitRateLimiters(key, collector);
        RateLimiter single = collector.single;
        List<RateLimiter> multi = collector.collection;
        LOG.trace("key = {}, limiter(s) = {}", key, single == null ? multi : single);
        if (single != null) {
            if (multi != null) {
                throw new AssertionError("May not have both single and multi");
            }
            return single;
        }
        if (multi == null || multi.isEmpty()) {
            return resultIfNone;
        }
        if (multi.size() == 1) {
            return multi.get(0) == null ? resultIfNone : multi.get(0);
        }
        return RateLimiters.of(multi.toArray(new RateLimiter[0]));
    }
}
