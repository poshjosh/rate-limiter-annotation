package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.AnnotationConverter;
import io.github.poshjosh.ratelimiter.annotation.RateId;
import io.github.poshjosh.ratelimiter.annotation.JavaRateSource;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;
import io.github.poshjosh.ratelimiter.util.Ticker;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

final class DefaultRateLimiterRegistry<K> implements RateLimiterRegistry<K> {

    private final RateLimiterContext<K> context;
    private final RootNodes<K> rootNodes;
    private final AnnotationConverter annotationConverter;

    DefaultRateLimiterRegistry(
            RateLimiterContext<K> context,
            RootNodes<K> rootNodes,
            AnnotationConverter annotationConverter) {
        this.context = Objects.requireNonNull(context);
        this.annotationConverter = Objects.requireNonNull(annotationConverter);
        this.rootNodes = Objects.requireNonNull(rootNodes);
    }

    public boolean isWithinLimit(K key) {
        if (!isRateLimitingSetup()) {
            return false;
        }
        final RateLimiterProvider provider = context.getRateLimiterProvider();
        final Ticker ticker = context.getTicker();
        final Node<MatchContext<K>> annoRoot = rootNodes.getAnnotationsRootNode();
        final Node<MatchContext<K>> propRoot = rootNodes.getPropertiesRootNode();

        if (!rootNodes.hasProperties()) {
            return NodeRateLimiter.isWithinLimit(provider, annoRoot, key, ticker);
        }
        if (!rootNodes.hasAnnotations()) {
            return NodeRateLimiter.isWithinLimit(provider, propRoot, key, ticker);
        }

        // properties comes before annotations
        final boolean propWithin = NodeRateLimiter.isWithinLimit(provider, propRoot, key, ticker);
        final boolean annoWithin = NodeRateLimiter.isWithinLimit(provider, annoRoot, key, ticker);

        return propWithin && annoWithin;
    }

    @Override
    public boolean tryAcquire(K key, int permits, long timeout, TimeUnit timeUnit) {
        if (!isRateLimitingSetup()) {
            return true;
        }
        final RateLimiterProvider provider = context.getRateLimiterProvider();
        final Node<MatchContext<K>> annoRoot = rootNodes.getAnnotationsRootNode();
        final Node<MatchContext<K>> propRoot = rootNodes.getPropertiesRootNode();

        if (!rootNodes.hasProperties()) {
            return NodeRateLimiter.tryAcquire(provider, annoRoot, key, permits, timeout, timeUnit);
        }
        if (!rootNodes.hasAnnotations()) {
            return NodeRateLimiter.tryAcquire(provider, propRoot, key, permits, timeout, timeUnit);
        }

        // properties comes before annotations
        final boolean propAcquired = NodeRateLimiter.tryAcquire(
                provider, propRoot, key, permits, timeout, timeUnit);
        final boolean annoAcquired = NodeRateLimiter.tryAcquire(
                provider, annoRoot, key, permits, timeout, timeUnit);

        return propAcquired && annoAcquired;
    }

    @Override
    public RateLimiterRegistry<K> register(String id, Rates rates) {
        if (isRegistered(id)) {
            return this;
        }
        addToPropertiesRoot(id, rates);
        return this;
    }

    @Override
    public RateLimiterRegistry<K> register(Class<?> source) {
        if (isRegistered(source)) {
            return this;
        }
        addToAnnotationsRoot(source);
        return this;
    }

    @Override
    public RateLimiterRegistry<K> register(Method source) {
        if (isRegistered(source)) {
            return this;
        }
        addToAnnotationsRoot(source);
        return this;
    }

    @Override
    public RateLimiter getRateLimiterOrUnlimited(K key) {
        final RateLimiter rateLimiter = getRateLimiterOrNull(key);
        return rateLimiter == null ? RateLimiters.NO_LIMIT : rateLimiter;
    }

    @Override
    public Optional<RateLimiter> getRateLimiterOptional(K key) {
        return Optional.ofNullable(getRateLimiterOrNull(key));
    }

    @Override
    public Optional<RateLimiter> getClassRateLimiterOptional(Class<?> clazz) {
        return Optional.ofNullable(getGenericRateLimiterOrNull(clazz));
    }

    @Override
    public Optional<RateLimiter> getMethodRateLimiterOptional(Method method) {
        return Optional.ofNullable(getGenericRateLimiterOrNull(method));
    }

    @Override
    public boolean isRegistered(String id) {
        final Object found = rootNodes.getPropertiesRootNode()
                .findFirstChildOrDefault(node -> isName(id, node), null);
        if (found != null) {
            return true;
        }
        return rootNodes.getAnnotationsRootNode()
                .findFirstChildOrDefault(node -> isName(id, node), null) != null;
    }

    @Override
    public boolean hasMatcher(String id) {
        final MatchContext<K> matchContext = getRateContextOrNull(id);
        return matchContext != null && matchContext.hasMatcher();
    }

    private RateLimiter getRateLimiterOrNull(K key) {
        if (!isRateLimitingSetup()) {
            return null;
        }
        if (!rootNodes.hasProperties()) {
            return createAnnotationsRateLimiter(key);
        }

        if (!rootNodes.hasAnnotations()) {
            return createPropertisRateLimiter(key);
        }
        // Properties take precedence over annotations
        return RateLimiters.of(createPropertisRateLimiter(key), createAnnotationsRateLimiter(key));
    }

    private boolean isRateLimitingSetup() {
        if (!context.isRateLimitingEnabled()) {
            return false;
        }
        return rootNodes.hasProperties() || rootNodes.hasAnnotations();
    }

    private RateLimiter createPropertisRateLimiter(K key){
        return new NodeRateLimiter<>(
                key, rootNodes.getPropertiesRootNode(), context.getRateLimiterProvider());
    }

    private RateLimiter createAnnotationsRateLimiter(K key){
        return new NodeRateLimiter<>(
                key, rootNodes.getAnnotationsRootNode(), context.getRateLimiterProvider());
    }

    private RateLimiter getGenericRateLimiterOrNull(GenericDeclaration source) {
        final String rateId = RateId.of(source);
        MatchContext<K> matchContext = getRateContextOrNull(rateId);
        if (matchContext == null) {
            final Node<MatchContext<K>> added = addToAnnotationsRoot(source);
            matchContext = added == null ? null : added.requireValue();
        }
        if (matchContext == null) {
            return null;
        }
        return getRateLimiterOrNull(rateId, matchContext);
    }

    private RateLimiter getRateLimiterOrNull(String key, MatchContext<K> matchContext) {
        if (!matchContext.getSource().isRateLimited()) {
            return null;
        }
        final Rates rates = matchContext.getRatesWithParentRatesAsFallback();
        return context.getRateLimiterProvider().getRateLimiter(key, rates);
    }

    private Node<MatchContext<K>> addToPropertiesRoot(String id, Rates rates) {
        final RateSource rateSource = RateSource.of(id, rates.isSet());
        final Node<RateConfig> node = createNodeOrNull(rateSource, rates);
        if (node == null) {
            return null;
        }
        return toRateContextNode(rootNodes.getAnnotationsRootNode(), node);
    }

    private Node<MatchContext<K>> addToAnnotationsRoot(GenericDeclaration source) {
        final RateSource rateSource = JavaRateSource.of(source);
        if (!rateSource.isRateLimited()) {
            return null;
        }
        final Rates rates = annotationConverter.convert(rateSource);
        final Node<RateConfig> node = createNodeOrNull(rateSource, rates);
        if (node == null) {
            return null;
        }
        return toRateContextNode(rootNodes.getAnnotationsRootNode(), node);
    }

    private MatchContext<K> getRateContextOrNull(String id) {
        Node<MatchContext<K>> node = getNodeOrNull(id);
        return node == null ? null : node.getValueOrDefault(null);
    }
    private Node<MatchContext<K>> getNodeOrNull(String id) {
        Node<MatchContext<K>> result = rootNodes.getPropertiesRootNode()
                .findFirstChildOrDefault(node -> isName(id, node), null);
        if (result != null) {
            return result;
        }
        return rootNodes.getAnnotationsRootNode()
                .findFirstChildOrDefault(node -> isName(id, node), null);
    }

    private <T> boolean isName(String id, Node<T> node) {
        return id.equals(node.getName());
    }

    private Node<MatchContext<K>> toRateContextNode(
            Node<MatchContext<K>> parent,
            Node<RateConfig> node) {
        // Child nodes are automatically added to the specified parent.
        return Nodes.of(node.getName(), toRateContext(node), parent);
    }

    private MatchContext<K> toRateContext(Node<RateConfig> node) {
        return MatchContexts.of(context.getMatcherProvider(), node);
    }

    private Node<RateConfig> createNodeOrNull(RateSource rateSource, Rates rates) {
        if (!rateSource.isRateLimited()) {
            return null;
        }
        return Nodes.of(rateSource.getId(), RateConfig.of(rateSource, rates));
    }
}
