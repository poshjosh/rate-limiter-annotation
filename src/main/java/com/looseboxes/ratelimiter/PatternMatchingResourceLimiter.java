package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class PatternMatchingResourceLimiter<V, R> implements ResourceLimiter<R> {

    private enum VisitResult {SUCCESS, FAILURE, NOMATCH}

    private static final Logger log = LoggerFactory.getLogger(PatternMatchingResourceLimiter.class);
    
    public interface MatcherProvider<V, T>{
        Matcher<T, ?> getMatcher(Node<NodeValue<V>> node);
    }
    
    public interface LimiterProvider<V>{
        ResourceLimiter<?> getLimiter(Node<NodeValue<V>> node);
    }

    private final MatcherProvider<V, R> matcherProvider;
    private final LimiterProvider<V> limiterProvider;
    private final Node<NodeValue<V>> rootNode;
    private final Set<Node<NodeValue<V>>> leafNodes;
    private final boolean firstMatchOnly;
    private UsageListener usageListener;

    public PatternMatchingResourceLimiter(MatcherProvider<V, R> matcherProvider,
                                          LimiterProvider<V> limiterProvider,
                                          Node<NodeValue<V>> rootNode,
                                          boolean firstMatchOnly) {
        this.matcherProvider = Objects.requireNonNull(matcherProvider);
        this.limiterProvider = Objects.requireNonNull(limiterProvider);
        this.rootNode = Objects.requireNonNull(rootNode);
        Set<Node<NodeValue<V>>> set = new LinkedHashSet<>();
        this.rootNode.visitAll(node -> {
            if (node.isLeaf()) {
                set.add(node);
            }
        });
        this.leafNodes = Collections.unmodifiableSet(set);
        this.firstMatchOnly = firstMatchOnly;
        this.usageListener = UsageListener.NO_OP;
    }

    @Override
    public ResourceLimiter<R> cache(RateCache<?> cache) {
        Objects.requireNonNull(cache);
        Map<Node<NodeValue<V>>, ResourceLimiter<?>> map = new HashMap<>();
        visitNodes(false, node -> {
            map.put(node, limiterProvider.getLimiter(node).cache(cache));
            return VisitResult.SUCCESS;
        });
        return new PatternMatchingResourceLimiter<>(matcherProvider, map::get, rootNode, firstMatchOnly);
    }

    @Override
    public ResourceLimiter<R> listener(UsageListener usageListener) {
        this.usageListener = Objects.requireNonNull(usageListener);
        Map<Node<NodeValue<V>>, ResourceLimiter<?>> map = new HashMap<>();
        visitNodes(false, node -> {
            map.put(node, limiterProvider.getLimiter(node).listener(usageListener));
            return VisitResult.SUCCESS;
        });
        return new PatternMatchingResourceLimiter<>(matcherProvider, map::get, rootNode, firstMatchOnly);
    }

    @Override
    public boolean tryConsume(R request, int permits, long timeout, TimeUnit unit) {
        Function<Node<NodeValue<V>>, VisitResult> consumePermits =
                node -> tryConsume(request, permits, timeout, unit, node);
        return visitNodes(firstMatchOnly, consumePermits);
    }

    public boolean visitNodes(boolean firstMatchOnly, Function<Node<NodeValue<V>>, VisitResult> visitor) {

        int globalFailureCount = 0;

        for(Node<NodeValue<V>> node : leafNodes) {

            int nodeSuccessCount = 0;

            while(node != rootNode && node != null && node.hasNodeValue()) {

                final VisitResult result = visitor.apply(node);

                switch(result) {
                    case SUCCESS: ++nodeSuccessCount; break;
                    case FAILURE: ++globalFailureCount; break;
                    case NOMATCH:
                        break;
                    default: throw new IllegalArgumentException();
                }

                if(!VisitResult.SUCCESS.equals(result)) {
                    break;
                }

                node = node.getParentOrDefault(null);
            }

            if(firstMatchOnly && nodeSuccessCount > 0) {
                break;
            }
        }

        return globalFailureCount == 0;
    }

    private VisitResult tryConsume(
            R request, int permits, long timeout, TimeUnit unit, Node<NodeValue<V>> node) {

        final ResourceLimiter<?> resourceLimiter = limiterProvider.getLimiter(node);

        if(resourceLimiter == ResourceLimiter.NO_OP) {
            return VisitResult.NOMATCH;
        }

        final Matcher<R, ?> matcher = matcherProvider.getMatcher(node);

        final Object resourceId = matcher.matchOrNull(request);

        if(log.isTraceEnabled()) {
            log.trace("Matched: {}, match: {}, name: {}, matcher: {}",
                    resourceId != null, resourceId, node.getName(), matcher);
        }

        if(resourceId == null) {
            return VisitResult.NOMATCH;
        }

        boolean success = ((ResourceLimiter)resourceLimiter).tryConsume(resourceId, permits, timeout, unit);

        NodeValue<V> nodeValue = node.getValueOrDefault(null);
        Object limit = nodeValue == null ? null : nodeValue.getValue();
        usageListener.onConsumed(request, permits, limit);

        if (!success) {
            usageListener.onRejected(request, permits, limit);
        }

        return success  ? VisitResult.SUCCESS : VisitResult.FAILURE;
    }
}
