package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Rates;
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
            return node -> Matcher.identity();
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
        @Override
        public ResourceLimiter<Object> getLimiter(Node<RateConfig> node) {
            return nameToLimiter.computeIfAbsent(node.getName(), key -> createLimiter(node));
        }
        private ResourceLimiter<Object> createLimiter(Node<RateConfig> node) {
            ResourceLimiter<Object> limiter = node.getValueOptional()
                    .map(RateConfig::getValue)
                    .filter(Rates::hasLimits)
                    .map(rates -> RateToBandwidthConverter.ofDefaults().convert(rates))
                    .map(ResourceLimiter::of)
                    .orElse(ResourceLimiter.NO_OP);
            return limiter;
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
                matcherProvider, limiterProvider, rootNode);
    }

    public static <R> ResourceLimiter<R> ofProperties(Node<RateConfig> rootNode) {
        return ofProperties(MatcherProvider.ofDefaults(), LimiterProvider.ofDefaults(), rootNode);
    }

    public static <R> ResourceLimiter<R> ofProperties(
            MatcherProvider<R> matcherProvider,
            LimiterProvider limiterProvider,
            Node<RateConfig> rootNode) {
        return new ResourceLimiterComposition<>(
                matcherProvider, limiterProvider, rootNode);
    }

    private enum VisitResult {NO_MATCH, LIMIT_NOT_SET, SUCCESS, FAILURE}

    private final MatcherProvider<R> matcherProvider;
    private final LimiterProvider limiterProvider;
    private final Node<RateConfig> rootNode;
    private final Collection<Node<RateConfig>> leafNodes;

    private ResourceLimiterComposition(
            MatcherProvider<R> matcherProvider,
            LimiterProvider limiterProvider,
            Node<RateConfig> rootNode) {
        this.matcherProvider = Objects.requireNonNull(matcherProvider);
        this.limiterProvider = Objects.requireNonNull(limiterProvider);
        this.rootNode = Objects.requireNonNull(rootNode);
        this.leafNodes = collectLeafNodes(rootNode);
    }
    private <V> Set<Node<V>> collectLeafNodes(Node<V> node) {
        Set<Node<V>> set = new LinkedHashSet<>();
        node.visitAll(Node::isLeaf, set::add);
        return Collections.unmodifiableSet(set);
    }

    @Override
    public boolean tryConsume(R request, int permits, long timeout, TimeUnit unit) {
        Function<Node<RateConfig>, VisitResult> consumePermits =
                node -> tryConsume(request, permits, timeout, unit, node);
        return visitNodes(consumePermits);
    }

    public boolean visitNodes(Function<Node<RateConfig>, VisitResult> visitor) {

        int globalFailureCount = 0;

        for(Node<RateConfig> node : leafNodes) {

            int nodeSuccessCount = 0;

            final VisitResult result = visitor.apply(node);

            log.debug("Result: {}, node: {}", result, node.getName());

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

    private VisitResult tryConsume(
            R request, int permits, long timeout, TimeUnit unit, Node<RateConfig> node) {

        final Matcher<R, ?> matcher = matcherProvider.getMatcher(node);

        final Object resourceId = matcher.matchOrNull(request);

        if(resourceId == null) {
            return VisitResult.NO_MATCH;
        }

        final Node<RateConfig> limiterNode = getGroupNode(node);

        final ResourceLimiter resourceLimiter = limiterProvider.getLimiter(limiterNode);

        log.trace("Limiter node: {}", limiterNode.getName());

        if(resourceLimiter == NO_OP) {
            return VisitResult.LIMIT_NOT_SET;
        }

        final boolean success = resourceLimiter.tryConsume(resourceId, permits, timeout, unit);

        return success ? VisitResult.SUCCESS : VisitResult.FAILURE;
    }

    private Node<RateConfig> getGroupNode(Node<RateConfig> node) {
        if (!node.isLeaf()) {
            return node;
        }
        Node<RateConfig> parent = node.getParentOrDefault(null);
        return parent.isRoot() ? node : parent;
    }
}
