package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.AnnotationConverter;
import io.github.poshjosh.ratelimiter.annotation.RateId;
import io.github.poshjosh.ratelimiter.annotation.JavaRateSource;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.*;

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
        final RateContext<K> rateContext = getRateContextOrNull(id);
        return rateContext != null && rateContext.hasMatcher();
    }

    private RateLimiter getRateLimiterOrNull(K key) {
        if (!context.isRateLimited()) {
            return null;
        }
        if (!rootNodes.hasProperties() && !rootNodes.hasAnnotations()) {
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

    private RateLimiter createPropertisRateLimiter(K key){
        if (RateContext.IS_BOTTOM_UP_TRAVERSAL) {
            return new RateLimiterCompositeBottomUp<>(key,
                    rootNodes.getPropertiesLeafNodes(), context.getRateLimiterProvider());
        }
        return new RateLimiterComposite<>(key,
                rootNodes.getPropertiesRootNode(), context.getRateLimiterProvider());
    }

    private RateLimiter createAnnotationsRateLimiter(K key){
        if (RateContext.IS_BOTTOM_UP_TRAVERSAL) {
            return new RateLimiterCompositeBottomUp<>(key,
                    rootNodes.getAnnotationsLeafNodes(), context.getRateLimiterProvider());
        }
        return new RateLimiterComposite<>(key,
                rootNodes.getAnnotationsRootNode(), context.getRateLimiterProvider());
    }

    private RateLimiter getGenericRateLimiterOrNull(GenericDeclaration source) {
        final String rateId = RateId.of(source);
        RateContext<K> rateContext = getRateContextOrNull(rateId);
        if (rateContext == null) {
            final Node<RateContext<K>> added = addToAnnotationsRoot(source);
            rateContext = added == null ? null : added.requireValue();
        }
        if (rateContext == null) {
            return null;
        }
        return getRateLimiterOrNull(rateId, rateContext);
    }

    private RateLimiter getRateLimiterOrNull(String key, RateContext<K> rateContext) {
        if (!rateContext.getSource().isRateLimited()) {
            return null;
        }
        final Rates rates = rateContext.getRatesWithParentRatesAsFallback();
        return context.getRateLimiterProvider().getRateLimiter(key, rates);
    }

    private Node<RateContext<K>> addToPropertiesRoot(String id, Rates rates) {
        final RateSource rateSource = RateSource.of(id, rates.isSet());
        final Node<RateConfig> node = createNodeOrNull(rateSource, rates);
        if (node == null) {
            return null;
        }
        return toRateContextNode(rootNodes.getAnnotationsRootNode(), node);
    }

    private Node<RateContext<K>> addToAnnotationsRoot(GenericDeclaration source) {
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

    private RateContext<K> getRateContextOrNull(String id) {
        Node<RateContext<K>> node = getNodeOrNull(id);
        return node == null ? null : node.getValueOrDefault(null);
    }
    private Node<RateContext<K>> getNodeOrNull(String id) {
        Node<RateContext<K>> result = rootNodes.getPropertiesRootNode()
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

    private Node<RateContext<K>> toRateContextNode(
            Node<RateContext<K>> parent,
            Node<RateConfig> node) {
        // Child nodes are automatically added to the specified parent.
        return Nodes.of(node.getName(), toRateContext(node), parent);
    }

    private RateContext<K> toRateContext(Node<RateConfig> node) {
        return RateContext.of(context.getMatcherProvider(), node);
    }

    private Node<RateConfig> createNodeOrNull(RateSource rateSource, Rates rates) {
        if (!rateSource.isRateLimited()) {
            return null;
        }
        return Nodes.of(rateSource.getId(), RateConfig.of(rateSource, rates));
    }
}
