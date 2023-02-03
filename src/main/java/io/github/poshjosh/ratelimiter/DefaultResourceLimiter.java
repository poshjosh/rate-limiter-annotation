package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.LimiterConfig;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.util.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A {@link io.github.poshjosh.ratelimiter.node.Node} is used to hold the data for either:
 *
 * <ul>
 *     <li>An annotation - In which case it is a group node</li>
 *     <li>A class</li>
 *     <li>A method</li>
 * </ul>
 *
 * Each {@link io.github.poshjosh.ratelimiter.node.Node} may have:
 *
 * <ul>
 *     <li>Zero or more rates defined</li>
 *     <li>A group annotation linking the node to a group</li>
 *     <li>Zero or more rates defined at the declaration of the annotation for the group</li>
 * </ul>
 *
 * Each node is identified in the store by a key, usually the match result returned by the node's
 * main {@link io.github.poshjosh.ratelimiter.util.Matcher}.
 *
 * <ul>
 *     <li>Group - the match result, otherwise the node name if match result is null</li>
 *     <li>Class - the match result</li>
 *     <li>Method - the match result</li>
 * </ul>
 *
 * In addition to each node's main {@link io.github.poshjosh.ratelimiter.util.Matcher}, ancillary
 * matchers may be defined. Ancillary matching takes effect when there is no composition.
 * Composition is the presence of multiple {@link io.github.poshjosh.ratelimiter.annotations.Rate}
 * annotations composed by an {@link io.github.poshjosh.ratelimiter.Operator} (e.g OR, AND, etc).
 *
 * A single {@link io.github.poshjosh.ratelimiter.node.Node} is passed to this class. From that node,
 * all the leaf nodes are collected. Every request is used to visit each leaf node. For each
 * leaf node, its parent node is visited recursively till the root node (which is not visited).
 *
 * @param <R> The type of the resource which is consumed
 */
final class DefaultResourceLimiter<R> implements ResourceLimiter<R> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultResourceLimiter.class);

    private enum VisitResult {NO_MATCH, LIMIT_NOT_SET, SUCCESS, FAILURE}

    private final UsageListener listener;
    private final LimiterProvider<R, Object> limiterProvider;
    private final Collection<Node<LimiterConfig<R, Object>>> leafNodes;

    DefaultResourceLimiter(
            UsageListener listener,
            LimiterProvider<R, Object> limiterProvider,
            Node<LimiterConfig<R, Object>> node) {
        this(listener, limiterProvider, collectLeafs(node));
    }
    private static <R> Collection<Node<LimiterConfig<R, Object>>> collectLeafs(Node<LimiterConfig<R, Object>> node) {
        Set<Node<LimiterConfig<R, Object>>> leafNodes = new LinkedHashSet<>();
        Predicate<Node<LimiterConfig<R, Object>>> test = n -> n.isLeaf() && !n.isRoot();
        node.getRoot().visitAll(test, leafNodes::add);
        return leafNodes;
    }

    private DefaultResourceLimiter(
            UsageListener listener,
            LimiterProvider<R, Object> limiterProvider,
            Collection<Node<LimiterConfig<R, Object>>> leafNodes) {
        this.listener = Objects.requireNonNull(listener);
        this.limiterProvider = Objects.requireNonNull(limiterProvider);
        this.leafNodes = Collections.unmodifiableCollection(leafNodes);
    }

    @Override public DefaultResourceLimiter<R> listener(UsageListener listener) {
        return new DefaultResourceLimiter<>(listener, limiterProvider, leafNodes);
    }

    @Override public UsageListener getListener() {
        return listener;
    }

    @Override
    public boolean tryConsume(R key, int permits, long timeout, TimeUnit unit) {
        Function<Node<LimiterConfig<R, Object>>, VisitResult> consumePermits =
                node -> startVisit(key, permits, timeout, unit, node);
        return visitNodes(consumePermits);
    }

    private boolean visitNodes(Function<Node<LimiterConfig<R, Object>>, VisitResult> visitor) {

        for(Node<LimiterConfig<R, Object>> node : leafNodes) {

            final VisitResult result = visitor.apply(node);

            LOG.trace("Result: {}, node: {}", result, node.getName());

            switch(result) {
                case NO_MATCH: continue;
                case SUCCESS:
                case LIMIT_NOT_SET: return true;
                case FAILURE: return false;
                default: throw new IllegalArgumentException("Unexpected visit result: " + result);
            }
        }

        return true;
    }

    private VisitResult startVisit(
            R request, int permits, long timeout, TimeUnit unit, Node<LimiterConfig<R, Object>> node) {
        if (!node.isLeaf()) {
            throw new AssertionError("Visiting must start with leaf nodes");
        }
        return visit(request, permits, timeout, unit, node, null);
    }

    private VisitResult visit(
            R request, int permits, long timeout, TimeUnit unit,
            Node<LimiterConfig<R, Object>> node, VisitResult previousResult) {

        if (node == null || node.isRoot()) {
            return previousResult;
        }

        Object match = matchOrNull(request, node);

        if (match == null && node.isLeaf()) {
            return VisitResult.NO_MATCH;
        }

        final Node<LimiterConfig<R, Object>> parent = node.getParentOrDefault(null);

        final LimiterConfig<R, Object> config = requireLimiterConfig(node);

        final Bandwidth [] bandwidths = config.getBandwidths();

        if (!hasLimits(node)) {
            return visit(request, permits, timeout, unit, parent, VisitResult.LIMIT_NOT_SET);
        }

        match = resolveMatch(match, node);

        final VisitResult result;
        if (!config.hasChildConditions()) {
            if (match == null) {
                result = node.isLeaf() ? VisitResult.NO_MATCH : previousResult;
            } else {
                result = tryAcquire(match, permits, timeout, unit, node)
                        ? VisitResult.SUCCESS : VisitResult.FAILURE;
            }
        } else {
            if (match == null) {
                result = node.isLeaf() ? VisitResult.NO_MATCH : previousResult;
            } else {
                result = visitMulti(request, match, permits, timeout, unit, node);
            }
        }

        if (match != null) {
            onVisit(match, permits, bandwidths, result);
        }

        if (parent == null || parent.isRoot()) {
            return result;
        }

        final VisitResult parentResult = visit(request, permits, timeout, unit, parent, result);

        return resolve(result, parentResult);
    }

    private Object matchOrNull(R request, Node<LimiterConfig<R, Object>> node) {
        final LimiterConfig<R, Object> config = requireLimiterConfig(node);
        final Matcher<R, Object> matcher = config.getMatcher();
        final Object match = matcher.matchOrNull(request);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Match: {}, node: {}, matcher: {}", match != null, node.getName(), matcher);
        }
        return match;
    }

    private Object resolveMatch(Object match, Node<LimiterConfig<R, Object>> node) {
        final LimiterConfig<R, Object> config = requireLimiterConfig(node);
        final SourceType sourceType = config.getSourceType();
        if (SourceType.GROUP.equals(config.getSourceType())) {
            return node.getName();
        }
        if (match == null && SourceType.PROPERTY.equals(sourceType) && !node.isLeaf()) {
            return node.getName();
        }
        return match;
    }

    private VisitResult resolve(VisitResult result, VisitResult parentResult) {
        if (VisitResult.NO_MATCH.equals(result) || VisitResult.LIMIT_NOT_SET.equals(result)) {
            return parentResult;
        }
        if (VisitResult.NO_MATCH.equals(parentResult) || VisitResult.LIMIT_NOT_SET.equals(parentResult)) {
            return result;
        }
        if (VisitResult.SUCCESS.equals(result) && VisitResult.SUCCESS.equals(parentResult)) {
            return VisitResult.SUCCESS;
        }
        return VisitResult.FAILURE;
    }

    private void onVisit(Object id, int permits, Bandwidth[] bandwidths, VisitResult result) {
        listener.onConsumed(id, permits, bandwidths);
        if (!VisitResult.FAILURE.equals(result)) {
            return;
        }
        listener.onRejected(id, permits, bandwidths);
    }

    private boolean tryAcquire(
            Object resourceId, int permits, long timeout, TimeUnit unit,
            Node<LimiterConfig<R, Object>> node) {
        LimiterConfig<R, Object> config = requireLimiterConfig(node);
        RateLimiter limiter = limiterProvider.getLimiters(resourceId, node.getName(), config).get(0);
        return tryAcquire(resourceId, limiter, permits, timeout, unit);
    }

    private VisitResult visitMulti(
            R request, Object resourceId, int permits, long timeout, TimeUnit unit,
            Node<LimiterConfig<R, Object>> node) {
        LimiterConfig<R, Object> config = requireLimiterConfig(node);
        final List<Matcher<R, Object>> matchers = config.getMatchers();

        final List<RateLimiter> limiters = limiterProvider.getLimiters(resourceId, node.getName(), config);

        int matchCount = 0;
        int successCount = 0;

        for (int i = 0; i < matchers.size(); i++) {

            final Object match = matchers.get(i).matchOrNull(request);

            if (match == null) {
                continue;
            }

            ++matchCount;

            if (tryAcquire(resourceId, limiters.get(i), permits, timeout, unit)) {
                ++successCount;
            }
        }

        if (matchCount == 0) {
            return VisitResult.NO_MATCH;
        }

        return matchCount == successCount ? VisitResult.SUCCESS : VisitResult.FAILURE;
    }

    private boolean tryAcquire(
            Object resourceId, RateLimiter rateLimiter, int permits, long timeout, TimeUnit unit) {

        final boolean acquired = rateLimiter.tryAcquire(permits, timeout, unit);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Limit exceeded: {}, for: {}", !acquired, resourceId);
        }

        return acquired;
    }

    private boolean hasLimits(Node<LimiterConfig<R, Object>> node) {
        return getRates(node).hasLimits();
    }

    private Rates getRates(Node<LimiterConfig<R, Object>> node) {
        return requireLimiterConfig(node).getRates();
    }

    private LimiterConfig<R, Object> requireLimiterConfig(Node<LimiterConfig<R, Object>> node) {
        return Objects.requireNonNull(node.getValueOrDefault(null));
    }

    @Override public String toString() {
        return "DefaultResourceLimiter{" + "leafNodes=" + leafNodes + '}';
    }
}
