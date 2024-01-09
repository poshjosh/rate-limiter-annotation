package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.BandwidthState;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.LimiterConfig;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Uses multiple {@link RateLimiter}s to restrict consumption of multiple resources identified by IDs.
 *
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
 * annotations composed by an {@link Operator} (e.g OR, AND, etc).
 *
 * A single {@link io.github.poshjosh.ratelimiter.node.Node} is passed to this class. From that node,
 * all the leaf nodes are collected. Every request is used to visit each leaf node. For each
 * leaf node, its parent node is visited recursively till the root node (which is not visited).
 *
 * @param <K> The type of the resource which is consumed
 */
final class DefaultResourceLimiter<K> implements ResourceLimiter<K> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultResourceLimiter.class);

    private enum VisitResult {NO_MATCH, LIMIT_NOT_SET, SUCCESS, FAILURE}

    private final UsageListener listener;
    private final RateLimiterProvider<K, String> rateLimiterProvider;
    private final Node<LimiterConfig<K>> rootNode;
    private final Collection<Node<LimiterConfig<K>>> leafNodes;

    DefaultResourceLimiter(
            UsageListener listener,
            RateLimiterProvider<K, String> rateLimiterProvider,
            Node<LimiterConfig<K>> node) {
        this(listener, rateLimiterProvider, node, collectLeafs(node));
    }
    private static <R> Collection<Node<LimiterConfig<R>>> collectLeafs(Node<LimiterConfig<R>> node) {
        Set<Node<LimiterConfig<R>>> leafNodes = new LinkedHashSet<>();
        Predicate<Node<LimiterConfig<R>>> test = n -> n.isLeaf() && n.hasValue();
        node.getRoot().visitAll(test, leafNodes::add);
        return leafNodes;
    }

    private DefaultResourceLimiter(
            UsageListener listener,
            RateLimiterProvider<K, String> rateLimiterProvider,
            Node<LimiterConfig<K>> rootNode,
            Collection<Node<LimiterConfig<K>>> leafNodes) {
        this.listener = Objects.requireNonNull(listener);
        this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
        this.rootNode = Objects.requireNonNull(rootNode);
        this.leafNodes = Collections.unmodifiableCollection(leafNodes);
    }

    @Override
    public List<BandwidthState> getBandwidths(K key) {
        final List<BandwidthState> bandwidths = new ArrayList<>();
        rootNode.visitAll(node -> {
            if (node == null) {
                return;
            }
            final LimiterConfig<K> config = node.getValueOrDefault(null);
            if (config == null) {
                return;
            }
            final String groupMatch = MatchUtil.match(node, key);
            if (config.hasChildConditions()) {
                final int count = config.getMatchers().size();
                for(int i = 0; i < count; i++) {
                    final String match = MatchUtil.matchAt(node, key, i, groupMatch);
                    if (Matcher.isMatch(match)) {
                        bandwidths.add(rateLimiterProvider.getRateLimiter(match, config, i).getBandwidth());
                    }
                }
            } else {
                if (Matcher.isMatch(groupMatch)) {
                    bandwidths.add(rateLimiterProvider.getRateLimiter(groupMatch, config).getBandwidth());
                }
            }
        });
        return Collections.unmodifiableList(bandwidths);
    }

    @Override public DefaultResourceLimiter<K> listener(UsageListener listener) {
        return new DefaultResourceLimiter<>(listener, rateLimiterProvider, rootNode, leafNodes);
    }

    @Override public UsageListener getListener() {
        return listener;
    }

    @Override
    public boolean tryConsume(K key, int permits, long timeout, TimeUnit unit) {

        for(Node<LimiterConfig<K>> node : leafNodes) {

            final VisitResult result = startVisit(key, permits, timeout, unit, node);

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
            K key, int permits, long timeout, TimeUnit unit, Node<LimiterConfig<K>> node) {
        if (!node.isLeaf()) {
            throw new AssertionError("Visiting must start with leaf nodes");
        }
        return visit(key, permits, timeout, unit, node, null);
    }

    private VisitResult visit(
            K key, int permits, long timeout, TimeUnit unit,
            Node<LimiterConfig<K>> node, VisitResult previousResult) {

        if (node == null || node.isRoot()) {
            return firstNonNull(previousResult, VisitResult.LIMIT_NOT_SET);
        }

        final String match = MatchUtil.match(node, key);
        final boolean matched = Matcher.isMatch(match);

        if (!matched && node.isLeaf()) {
            return VisitResult.NO_MATCH;
        }

        final LimiterConfig<K> config = requireLimiterConfig(node);
        final Node<LimiterConfig<K>> parent = node.getParentOrDefault(null);

        if (!config.hasLimits()) {
            return visit(key, permits, timeout, unit, parent, VisitResult.LIMIT_NOT_SET);
        }

        final VisitResult result;
        if (!config.hasChildConditions()) {
            if (!matched) {
                result = node.isLeaf() ? VisitResult.NO_MATCH :
                        firstNonNull(previousResult, VisitResult.NO_MATCH);
            } else {
                result = visitSingle(match, permits, timeout, unit, config);
            }
        } else {
            if (!matched) {
                result = node.isLeaf() ? VisitResult.NO_MATCH :
                        firstNonNull(previousResult, VisitResult.NO_MATCH);
            } else {
                result = visitMulti(key, match, permits, timeout, unit, node);
            }
        }

        if (matched) {
            onVisit(key, match, permits, config, result);
        }

        if (parent == null || parent.isRoot()) {
            return result;
        }

        final VisitResult parentResult = visit(key, permits, timeout, unit, parent, result);

        return resolve(result, parentResult);
    }

    private VisitResult firstNonNull(VisitResult result, VisitResult alternate) {
        return result == null ? alternate : result;
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

    private void onVisit(Object key, String groupMatch, int permits, LimiterConfig<K> config, VisitResult result) {
        listener.onConsumed(key, groupMatch, permits, config);
        if (!VisitResult.FAILURE.equals(result)) {
            return;
        }
        listener.onRejected(key, groupMatch, permits, config);
    }

    private VisitResult visitSingle(
            String groupMatch, int permits, long timeout, TimeUnit unit, LimiterConfig<K> config) {
        RateLimiter limiter = rateLimiterProvider.getRateLimiter(groupMatch, config);
        return tryAcquire(groupMatch, limiter, permits, timeout, unit)
                ? VisitResult.SUCCESS : VisitResult.FAILURE;
    }

    private VisitResult visitMulti(
            K key, String groupMatch, int permits, long timeout, TimeUnit unit,
            Node<LimiterConfig<K>> node) {

        final LimiterConfig<K> config = requireLimiterConfig(node);
        
        final List<Matcher<K>> matchers = config.getMatchers();

        int matchCount = 0;
        int successCount = 0;

        for (int i = 0; i < matchers.size(); i++) {

            final String id = MatchUtil.matchAt(node, key, i, groupMatch);

            if (!Matcher.isMatch(id)) {
                continue;
            }

            ++matchCount;

            RateLimiter limiter = rateLimiterProvider.getRateLimiter(id, config, i);

            if (tryAcquire(id, limiter, permits, timeout, unit)) {
                ++successCount;
            }
        }

        if (matchCount == 0) {
            return VisitResult.NO_MATCH;
        }

        return matchCount == successCount ? VisitResult.SUCCESS : VisitResult.FAILURE;
    }

    private boolean tryAcquire(
            Object groupMatch, RateLimiter rateLimiter,
            int permits, long timeout, TimeUnit unit) {

        final boolean acquired = rateLimiter.tryAcquire(permits, timeout, unit);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Limit exceeded: {}, for: {}, bandwidth: {}",
                    !acquired, groupMatch, rateLimiter.getBandwidth());
        }

        return acquired;
    }

    private LimiterConfig<K> requireLimiterConfig(Node<LimiterConfig<K>> node) {
        return Objects.requireNonNull(node.getValueOrDefault(null));
    }

    @Override
    public String toString() {
        return "DefaultResourceLimiter{rootNode=" + rootNode + '}';
    }
}
