package com.looseboxes.ratelimiter;

import com.looseboxes.ratelimiter.annotation.RateConfig;
import com.looseboxes.ratelimiter.cache.RateCache;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class MatchedResourceLimiter<R> implements ResourceLimiter<R> {

    public static <R> ResourceLimiter<R> ofAnnotations(
            MatcherProvider<R> matcherProvider,
            LimiterProvider limiterProvider,
            Node<RateConfig> rootNode) {
        return new MatchedResourceLimiter<>(
                matcherProvider, limiterProvider, rootNode, false);
    }

    public static <R> ResourceLimiter<R> ofProperties(
            MatcherProvider<R> matcherProvider,
            LimiterProvider limiterProvider,
            Node<RateConfig> rootNode) {
        return new MatchedResourceLimiter<>(
                matcherProvider, limiterProvider, rootNode, true);
    }

    private enum VisitResult {SUCCESS, FAILURE, NOMATCH}

    private static final Logger log = LoggerFactory.getLogger(MatchedResourceLimiter.class);
    
    public interface MatcherProvider<T>{
        Matcher<T, ?> getMatcher(Node<RateConfig> node);
    }
    
    public interface LimiterProvider{
        ResourceLimiter<?> getLimiter(Node<RateConfig> node);
    }

    private final MatcherProvider<R> matcherProvider;
    private final LimiterProvider limiterProvider;
    private final Node<RateConfig> rootNode;
    private final Set<Node<RateConfig>> leafNodes;
    private final boolean firstMatchOnly;
    private UsageListener usageListener;

    private MatchedResourceLimiter(MatcherProvider<R> matcherProvider,
                                          LimiterProvider limiterProvider,
                                          Node<RateConfig> rootNode,
                                          boolean firstMatchOnly) {
        this.matcherProvider = Objects.requireNonNull(matcherProvider);
        this.limiterProvider = Objects.requireNonNull(limiterProvider);
        this.rootNode = Objects.requireNonNull(rootNode);
        Set<Node<RateConfig>> set = new LinkedHashSet<>();
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
        Map<Node<RateConfig>, ResourceLimiter<?>> map = new HashMap<>();
        visitNodes(false, node -> {
            map.put(node, limiterProvider.getLimiter(node).cache(cache));
            return VisitResult.SUCCESS;
        });
        return new MatchedResourceLimiter<>(matcherProvider, map::get, rootNode, firstMatchOnly);
    }

    @Override
    public ResourceLimiter<R> listener(UsageListener usageListener) {
        this.usageListener = Objects.requireNonNull(usageListener);
        Map<Node<RateConfig>, ResourceLimiter<?>> map = new HashMap<>();
        visitNodes(false, node -> {
            map.put(node, limiterProvider.getLimiter(node).listener(usageListener));
            return VisitResult.SUCCESS;
        });
        return new MatchedResourceLimiter<>(matcherProvider, map::get, rootNode, firstMatchOnly);
    }

    @Override
    public boolean tryConsume(R request, int permits, long timeout, TimeUnit unit) {
        Function<Node<RateConfig>, VisitResult> consumePermits =
                node -> tryConsume(request, permits, timeout, unit, node);
        return visitNodes(firstMatchOnly, consumePermits);
    }

    public boolean visitNodes(boolean firstMatchOnly, Function<Node<RateConfig>, VisitResult> visitor) {

        int globalFailureCount = 0;

        for(Node<RateConfig> node : leafNodes) {

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
            R request, int permits, long timeout, TimeUnit unit, Node<RateConfig> node) {

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

        final boolean success = ((ResourceLimiter)resourceLimiter)
                .tryConsume(resourceId, permits, timeout, unit);

        final RateConfig rateConfig = node.getValueOrDefault(null);
        final Object limit = rateConfig == null ? null : rateConfig.getValue();
        usageListener.onConsumed(request, permits, limit);

        if (success) {
            return VisitResult.SUCCESS;
        }

        usageListener.onRejected(request, permits, limit);

        return VisitResult.FAILURE;
    }
}
