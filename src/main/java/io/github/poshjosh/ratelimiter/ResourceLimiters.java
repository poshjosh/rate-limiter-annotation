package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.matcher.ExpressionMatcher;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.Rates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class ResourceLimiters<R> implements ResourceLimiter<R> {

    private static final Logger log = LoggerFactory.getLogger(ResourceLimiters.class);

    public interface MatcherProvider<T>{
        static <T> MatcherProvider<T> ofDefaults() {
            return new DefaultMatcherProvider<>();
        }
        Matcher<T, ?> getMatcher(Node<RateConfig> node);
    }

    private static final class NodeNameMatcher<T> implements Matcher<T, String> {
        private final String parentName;
        private final String name;
        public NodeNameMatcher(Node<RateConfig> node) {
            this.parentName = node.getParentOptional()
                    .filter(parent -> !parent.isRoot()).map(Node::getName).orElse(null);
            this.name = node.isRoot() ? null : node.getName();
        }
        @Override
        public String matchOrNull(T target) {
            return Objects.equals(name, target) || Objects.equals(parentName, target)? name : null;
        }
    }

    private static final class DefaultMatcherProvider<T> implements MatcherProvider<T> {
        private final Map<String, Matcher<T,?>> nameToMatcher = new HashMap<>();
        private final ExpressionMatcher<T, Object> sysExpressionMatcher;
        private DefaultMatcherProvider() {
            sysExpressionMatcher = ExpressionMatcher.ofSystem();
        }
        @Override public Matcher<T, ?> getMatcher(Node<RateConfig> node) {
            return nameToMatcher.computeIfAbsent(node.getName(), k -> createMatcher(node));
        }
        private Matcher<T, ?> createMatcher(Node<RateConfig> node) {
            final String expression = node.getValueOptional()
                    .map(RateConfig::getValue)
                    .map(Rates::getRateCondition)
                    .orElse("");
            if (expression.isEmpty()) {
                return new NodeNameMatcher<>(node);
            }
            if (sysExpressionMatcher.isSupported(expression)) {
                return sysExpressionMatcher.with(expression);
            } else {
                return new NodeNameMatcher<>(node);
            }
        }
    }

    public interface LimiterProvider{
        static LimiterProvider ofDefaults() {
            return new DefaultLimiterProvider();
        }
        ResourceLimiter<?> getLimiter(Node<RateConfig> node);
    }

    private static final class DefaultLimiterProvider implements LimiterProvider{
        private final Map<String, ResourceLimiter<Object>> nameToLimiter = new HashMap<>();
        private DefaultLimiterProvider() {}
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

    public static <R> ResourceLimiter<R> of(Class<?>... sourceOfRateLimitInfo) {
        return of(MatcherProvider.ofDefaults(), LimiterProvider.ofDefaults(),
                RateProcessor.ofDefaults(), sourceOfRateLimitInfo);
    }

    public static <R> ResourceLimiter<R> of(Node<RateConfig> rootNode) {
        return of(MatcherProvider.ofDefaults(), LimiterProvider.ofDefaults(), rootNode);
    }

    public static <R> ResourceLimiter<R> of(
            MatcherProvider<R> matcherProvider,
            LimiterProvider limiterProvider,
            Node<RateConfig> rootNode) {
        return new ResourceLimiters<>(matcherProvider, limiterProvider, rootNode);
    }

    public static <R> ResourceLimiter<R> of(
            MatcherProvider<R> matcherProvider,
            LimiterProvider limiterProvider,
            Class<?>... sourceOfRateLimitInfo) {
        return of(matcherProvider, limiterProvider, RateProcessor.ofDefaults(), sourceOfRateLimitInfo);
    }

    public static <S, R> ResourceLimiter<R> of(
            MatcherProvider<R> matcherProvider,
            LimiterProvider limiterProvider,
            RateProcessor<S> rateProcessor,
            S... sourceOfRateLimitInfo) {
        Node<RateConfig> rootNode = rateProcessor.processAll(sourceOfRateLimitInfo);
        return new ResourceLimiters<>(matcherProvider, limiterProvider, rootNode);
    }

    private enum VisitResult {NO_MATCH, LIMIT_NOT_SET, SUCCESS, FAILURE}

    private final MatcherProvider<R> matcherProvider;
    private final LimiterProvider limiterProvider;
    private final Collection<Node<RateConfig>> leafNodes;

    ResourceLimiters(
            MatcherProvider<R> matcherProvider,
            LimiterProvider limiterProvider,
            Node<RateConfig> rootNode) {
        this.matcherProvider = Objects.requireNonNull(matcherProvider);
        this.limiterProvider = Objects.requireNonNull(limiterProvider);
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

    private VisitResult tryConsume(
            R request, int permits, long timeout, TimeUnit unit, Node<RateConfig> node) {

        final Matcher<R, ?> matcher = matcherProvider.getMatcher(node);

        final Object resourceId = matcher.matchOrNull(request);

        if(resourceId == null) {
            return VisitResult.NO_MATCH;
        }

        final VisitResult result = doTryConsume(resourceId, permits, timeout, unit, node);

        final Node<RateConfig> groupNode = getGroupNodeOrNull(node);

        if (groupNode == null) {
            return result;
        }

        final VisitResult groupResult = doTryConsume(resourceId, permits, timeout, unit, groupNode);

        if (VisitResult.LIMIT_NOT_SET.equals(groupResult)) {
            return result;
        }

        if (VisitResult.LIMIT_NOT_SET.equals(result)) {
            return groupResult;
        }

        if (VisitResult.SUCCESS.equals(result) && VisitResult.SUCCESS.equals(groupResult)) {
            return VisitResult.SUCCESS;
        }

        return VisitResult.FAILURE;
    }

    private VisitResult doTryConsume(
            Object resourceId, int permits, long timeout, TimeUnit unit, Node<RateConfig> limiterNode) {

        final ResourceLimiter resourceLimiter = limiterProvider.getLimiter(limiterNode);

        log.trace("Limiter node: {}", limiterNode.getName());

        if(resourceLimiter == NO_OP) {
            return VisitResult.LIMIT_NOT_SET;
        }

        final boolean success = resourceLimiter.tryConsume(resourceId, permits, timeout, unit);

        return success ? VisitResult.SUCCESS : VisitResult.FAILURE;

    }

    private Node<RateConfig> getGroupNodeOrNull(Node<RateConfig> node) {
        Node<RateConfig> closestParentToRoot = getClosestParentToRootOrNull(node);
        return node.getName().equals(closestParentToRoot.getName()) ? null : closestParentToRoot;
    }

    private Node<RateConfig> getClosestParentToRootOrNull(Node<RateConfig> node) {
        Node<RateConfig> parent = node.getParentOrDefault(null);
        return parent == null || parent.isRoot() ? node : getClosestParentToRootOrNull(parent);
    }
}
