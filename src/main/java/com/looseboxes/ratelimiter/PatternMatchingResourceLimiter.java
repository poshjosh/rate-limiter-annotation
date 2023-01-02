package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
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
    }

    @Override
    public <K> ResourceLimiter<R> keyProvider(KeyProvider<R, K> keyProvider) {
        Map<Node<NodeValue<V>>, ResourceLimiter<?>> map = new HashMap<>();
        visitNodes(false, node -> {
            map.put(node, limiterProvider.getLimiter(node).keyProvider((KeyProvider) keyProvider));
            return VisitResult.SUCCESS;
        });
        return new PatternMatchingResourceLimiter<>(matcherProvider, map::get, rootNode, firstMatchOnly);
    }

    @Override
    public <K> ResourceLimiter<R> cache(RateCache<K, Bandwidths> cache) {
        Map<Node<NodeValue<V>>, ResourceLimiter<?>> map = new HashMap<>();
        visitNodes(false, node -> {
            map.put(node, limiterProvider.getLimiter(node).cache(cache));
            return VisitResult.SUCCESS;
        });
        return new PatternMatchingResourceLimiter<>(matcherProvider, map::get, rootNode, firstMatchOnly);
    }

    @Override
    public ResourceLimiter<R> listener(ResourceUsageListener usageListener) {
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

        final Object match = matcher.matchOrNull(request);

        if(log.isTraceEnabled()) {
            log.trace("Matched: {}, match: {}, name: {}, matcher: {}",
                    match != null, match, node.getName(), matcher);
        }

        if(match == null) {
            return VisitResult.NOMATCH;
        }

        return ((ResourceLimiter)resourceLimiter)
                .tryConsume(match, permits, timeout, unit)
                ? VisitResult.SUCCESS : VisitResult.FAILURE;
    }
}
