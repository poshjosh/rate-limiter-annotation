package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.util.Rates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

final class DefaultResourceLimiter<R> implements ResourceLimiter<R> {

    private static final Logger log = LoggerFactory.getLogger(DefaultResourceLimiter.class);

    private enum VisitResult {NO_MATCH, LIMIT_NOT_SET, SUCCESS, FAILURE}

    private final MatcherProvider<R> matcherProvider;
    private final Collection<Node<RateConfig>> leafNodes;
    private final UsageListener listener;
    private final BandwidthsContext<Object> context;

    DefaultResourceLimiter(
            UsageListener listener,
            BandwidthsContext<?> context,
            MatcherProvider<R> matcherProvider,
            Node<RateConfig> node) {
        this(listener, context, matcherProvider, collectLeafs(node));
    }
    private static Collection<Node<RateConfig>> collectLeafs(Node<RateConfig> node) {
        Set<Node<RateConfig>> leafNodes = new LinkedHashSet<>();
        node.getRoot().visitAll(Node::isLeaf, leafNodes::add);
        return leafNodes;
    }

    private DefaultResourceLimiter(
            UsageListener listener,
            BandwidthsContext<?> context,
            MatcherProvider<R> matcherProvider,
            Collection<Node<RateConfig>> leafNodes) {
        this.listener = Objects.requireNonNull(listener);
        this.context = (BandwidthsContext<Object>)Objects.requireNonNull(context);
        this.matcherProvider = Objects.requireNonNull(matcherProvider);
        this.leafNodes = Collections.unmodifiableCollection(leafNodes);
    }

    @Override public DefaultResourceLimiter<R> listener(UsageListener listener) {
        return new DefaultResourceLimiter<>(listener, context, matcherProvider, leafNodes);
    }

    @Override public UsageListener getListener() {
        return listener;
    }

    @Override
    public boolean tryConsume(R key, int permits, long timeout, TimeUnit unit) {
        Function<Node<RateConfig>, VisitResult> consumePermits =
                node -> visit(key, permits, timeout, unit, node);
        return visitNodes(consumePermits);
    }

    private boolean visitNodes(Function<Node<RateConfig>, VisitResult> visitor) {

        int globalFailureCount = 0;

        for(Node<RateConfig> node : leafNodes) {

            int nodeSuccessCount = 0;

            final VisitResult result = visitor.apply(node);

            log.trace("Result: {}, node: {}", result, node.getName());

            switch(result) {
                case SUCCESS: ++nodeSuccessCount; break;
                case FAILURE: ++globalFailureCount; break;
                case NO_MATCH:
                case LIMIT_NOT_SET:
                    break;
                default: throw new IllegalArgumentException("Unexpected visit result: " + result);
            }

            if(nodeSuccessCount > 0) {
                break;
            }
        }

        return globalFailureCount == 0;
    }

    private VisitResult visit(
            R request, int permits, long timeout, TimeUnit unit, Node<RateConfig> node) {

        Object match = match(request, node);

        final VisitResult result = match == null ? VisitResult.NO_MATCH :
                doVisit(match, permits, timeout, unit, node);

        final Node<RateConfig> groupNode = getGroupNodeOrNull(node);

        if (groupNode == null) {
            return result;
        }

        if (match == null) {
            match = match(request, groupNode);
        }

        if (match == null) {
            return VisitResult.NO_MATCH;
        }

        final VisitResult gpResult = doVisit(match, permits, timeout, unit, groupNode);

        if (VisitResult.NO_MATCH.equals(result) || VisitResult.LIMIT_NOT_SET.equals(result)) {
            return gpResult;
        }

        if (VisitResult.NO_MATCH.equals(gpResult) || VisitResult.LIMIT_NOT_SET.equals(gpResult)) {
            return result;
        }

        if (VisitResult.SUCCESS.equals(result) || VisitResult.SUCCESS.equals(gpResult)) {
            return VisitResult.SUCCESS;
        }

        return VisitResult.FAILURE;
    }

    private Object match(R request, Node<RateConfig> node) {

        final Matcher<R, ?> matcher = matcherProvider.getMatcher(node.getName(), getRateConfig(node));

        final Object resourceId = matcher.matchOrNull(request);

        log.trace("Match: {}, node: {}", resourceId, node.getName());

        return resourceId;
    }

    private VisitResult doVisit(
            Object id, int permits, long timeout, TimeUnit unit, Node<RateConfig> node) {

        if (!hasLimits(node)) {
            return VisitResult.LIMIT_NOT_SET;
        }

        final boolean success = tryAcquire(id, node, permits, timeout, unit);

        return success ? VisitResult.SUCCESS : VisitResult.FAILURE;

    }

    private <V> Node<V> getGroupNodeOrNull(Node<V> node) {
        Node<V> closestParentToRoot = getClosestParentToRootOrNull(node);
        return node.getName().equals(closestParentToRoot.getName()) ? null : closestParentToRoot;
    }

    private <V> Node<V> getClosestParentToRootOrNull(Node<V> node) {
        Node<V> parent = node.getParentOrDefault(null);
        return parent == null || parent.isRoot() ? node : getClosestParentToRootOrNull(parent);
    }

    private boolean tryAcquire(
            Object resourceId, Node<RateConfig> node, int permits, long timeout, TimeUnit unit) {

        final Bandwidths bandwidths = context.getBandwidths(resourceId, getRateConfig(node));

        final RateLimiter rateLimiter = context.getLimiter(resourceId, bandwidths);

        final boolean acquired = rateLimiter.tryAcquire(permits, timeout, unit);

        if(acquired) {
            context.onChange(resourceId, bandwidths);
        }

        if(log.isTraceEnabled()) {
            log.trace("Limit exceeded: {}, for: {}, limit: {}", !acquired, resourceId, bandwidths);
        }

        listener.onConsumed(resourceId, permits, bandwidths);

        if (acquired) {
            return true;
        }

        listener.onRejected(resourceId, permits, bandwidths);

        return false;
    }
    
    private boolean hasLimits(Node<RateConfig> node) {
        return getRates(node).hasLimits();
    }
    
    private Rates getRates(Node<RateConfig> node) {
        return getRateConfig(node).getValue();
    }

    private static final RateConfig NONE = RateConfig.of(Rates.empty());
    private RateConfig getRateConfig(Node<RateConfig> node) {
        return node.getValueOrDefault(NONE);
    }
}
