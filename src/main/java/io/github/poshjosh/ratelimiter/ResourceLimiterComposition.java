package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class ResourceLimiterComposition<R> implements ResourceLimiter<R> {

    private static final Logger log = LoggerFactory.getLogger(ResourceLimiterComposition.class);

    public interface MatcherProvider<T>{
        // Provides a matcher that will match all requests
        static <T> MatcherProvider<T> ofDefaults() {
            return node -> key -> key + "-" + node.getName();
        }
        Matcher<T, ?> getMatcher(Node<RateConfig> node);
    }

    public interface LimiterProvider{
        static LimiterProvider ofDefaults() {
            return new DefaultLimiterProvider();
        }
        ResourceLimiter<?> getLimiter(Node<RateConfig> node);
    }

    private static final class DefaultLimiterProvider implements LimiterProvider{
        private final Map<String, ResourceLimiter<Object>> nameToLimiter = new HashMap<>();
        public ResourceLimiter<Object> getLimiter(Node<RateConfig> node) {
            return nameToLimiter.computeIfAbsent(node.getName(), k -> createLimiter(node));
        }
        private ResourceLimiter<Object> createLimiter(Node<RateConfig> node) {
            return node.getValueOptional()
                    .map(RateConfig::getValue)
                    .map(rates -> RateToBandwidthConverter.ofDefaults().convert(rates))
                    .map(ResourceLimiter::of)
                    .orElse(ResourceLimiter.NO_OP);
        }
    }

    public static <R> ResourceLimiter<R> ofAnnotations(Node<RateConfig> rootNode) {
        return ofAnnotations(MatcherProvider.ofDefaults(), LimiterProvider.ofDefaults(), rootNode);
    }

    public static <R> ResourceLimiter<R> ofAnnotations(
            MatcherProvider<R> matcherProvider,
            LimiterProvider limiterProvider,
            Node<RateConfig> rootNode) {
        return new ResourceLimiterComposition<>(
                matcherProvider, limiterProvider, rootNode, false);
    }

    public static <R> ResourceLimiter<R> ofProperties(Node<RateConfig> rootNode) {
        return ofProperties(MatcherProvider.ofDefaults(), LimiterProvider.ofDefaults(), rootNode);
    }

    public static <R> ResourceLimiter<R> ofProperties(
            MatcherProvider<R> matcherProvider,
            LimiterProvider limiterProvider,
            Node<RateConfig> rootNode) {
        return new ResourceLimiterComposition<>(
                matcherProvider, limiterProvider, rootNode, true);
    }

    private enum VisitResult {SUCCESS, FAILURE, NOMATCH}

    private final MatcherProvider<R> matcherProvider;
    private final LimiterProvider limiterProvider;
    private final Node<RateConfig> rootNode;
    private final Set<Node<RateConfig>> leafNodes;
    private final boolean firstMatchOnly;

    private ResourceLimiterComposition(
            MatcherProvider<R> matcherProvider, LimiterProvider limiterProvider,
            Node<RateConfig> rootNode, boolean firstMatchOnly) {
        this.matcherProvider = Objects.requireNonNull(matcherProvider);
        this.limiterProvider = Objects.requireNonNull(limiterProvider);
        this.rootNode = Objects.requireNonNull(rootNode);
        this.leafNodes = collectLeafNodes(rootNode);
        this.firstMatchOnly = firstMatchOnly;
    }
    private Set<Node<RateConfig>> collectLeafNodes(Node<RateConfig> rootNode) {
        Set<Node<RateConfig>> set = new LinkedHashSet<>();
        rootNode.visitAll(Node::isLeaf, set::add);
        return Collections.unmodifiableSet(set);
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

        if(resourceLimiter == NO_OP) {
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

        if (success) {
            return VisitResult.SUCCESS;
        }

        return VisitResult.FAILURE;
    }
}
