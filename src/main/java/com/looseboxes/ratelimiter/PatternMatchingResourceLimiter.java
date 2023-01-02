package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class PatternMatchingResourceLimiter<V, R> implements ResourceLimiter<R> {

    private enum RateLimitResult{SUCCESS, FAILURE, NOMATCH}

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
    public boolean tryConsume(Object context, R request, int permits, long timeout, TimeUnit unit) {
        Function<Node<NodeValue<V>>, RateLimitResult> consumePermits =
                node -> tryConsume(request, permits, timeout, unit, node);
        return visitNodes(consumePermits);
    }

    public boolean visitNodes(Function<Node<NodeValue<V>>, RateLimitResult> visitor) {

        int globalFailureCount = 0;

        for(Node<NodeValue<V>> node : leafNodes) {

            int nodeSuccessCount = 0;

            while(node != rootNode && node != null && node.hasNodeValue()) {

                final RateLimitResult result = visitor.apply(node);

                switch(result) {
                    case SUCCESS: ++nodeSuccessCount; break;
                    case FAILURE: ++globalFailureCount; break;
                    case NOMATCH:
                        break;
                    default: throw new IllegalArgumentException();
                }

                if(!RateLimitResult.SUCCESS.equals(result)) {
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

    private RateLimitResult tryConsume(
            R request, int permits, long timeout, TimeUnit unit, Node<NodeValue<V>> node) {

        final ResourceLimiter<?> resourceLimiter = limiterProvider.getLimiter(node);

        if(resourceLimiter == ResourceLimiter.NO_OP) {
            return RateLimitResult.NOMATCH;
        }

        final Matcher<R, ?> matcher = matcherProvider.getMatcher(node);

        final Object match = matcher.matchOrNull(request);

        if(log.isTraceEnabled()) {
            log.trace("Matched: {}, match: {}, name: {}, matcher: {}",
                    match != null, match, node.getName(), matcher);
        }

        if(match == null) {
            return RateLimitResult.NOMATCH;
        }

        return ((ResourceLimiter<Object>) resourceLimiter)
                .tryConsume(request, match, permits, timeout, unit)
                ? RateLimitResult.SUCCESS : RateLimitResult.FAILURE;
    }
}
