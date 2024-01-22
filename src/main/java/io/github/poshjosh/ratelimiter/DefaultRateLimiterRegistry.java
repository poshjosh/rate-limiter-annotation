package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.AnnotationConverter;
import io.github.poshjosh.ratelimiter.annotation.RateId;
import io.github.poshjosh.ratelimiter.annotation.JavaRateSource;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;

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
    public RateLimiterRegistry<K> register(Class<?> source) {
        if (isRegistered(source)) {
            return this;
        }
        addRateContextToAnnotationsRoot(source);
        return this;
    }

    @Override
    public RateLimiterRegistry<K> register(Method source) {
        if (isRegistered(source)) {
            return this;
        }
        addRateContextToAnnotationsRoot(source);
        return this;
    }

    @Override
    public RateLimiterFactory<K> createRateLimiterFactory() {
        return RateLimiterFactoryCreator.create(context, rootNodes);
    }

    @Override
    public Optional<RateLimiter> getRateLimiter(Class<?> clazz) {
        return getOrCreateRateLimiter(clazz);
    }

    @Override
    public Optional<RateLimiter> getRateLimiter(Method method) {
        return getOrCreateRateLimiter(method);
    }

    @Override
    public boolean isRegistered(String id) {
        return rootNodes.getPropertiesRootNode().findFirstChild(node -> isName(id, node)).isPresent()
                || rootNodes.getAnnotationsRootNode().findFirstChild(node -> isName(id, node)).isPresent();
    }

    @Override
    public boolean hasMatcher(String id) {
        return getRateContext(id).filter(RateContext::hasMatcher).isPresent();
    }

    private Optional<RateLimiter> getOrCreateRateLimiter(GenericDeclaration source) {
        final String rateId = RateId.of(source);
        RateContext<K> context = getRateContext(rateId)
                .orElseGet(() -> addRateContextToAnnotationsRoot(source).orElse(null));
        if (context == null) {
            return Optional.empty();
        }
        return getRateLimiter(rateId, context);
    }

    private Optional<RateLimiter> getRateLimiter(String key, RateContext<K> rateContext) {
        // This is faster, but will not work if the @Rate annotation is not
        // present on the class or method. (e.g. it is present on a @RateGroup)
        //if (!rateContext.getRates().hasLimits())
        if (!rateContext.getSource().isRateLimited()) {
            return Optional.empty();
        }
        final Rates rates = rateContext.getRatesWithParentRatesAsFallback();
        return Optional.of(context.getRateLimiterProvider().getRateLimiter(key, rates));
    }

    private Optional<RateContext<K>> addRateContextToAnnotationsRoot(GenericDeclaration source) {
        Node<RateContext<K>> parent = rootNodes.getAnnotationsRootNode();
        return createNode(source, null)
                .map(node -> toRateContextNode(parent, node).requireValue());
    }
    private Optional<RateContext<K>> getRateContext(String id) {
        RateContext<K> rateContext = rootNodes.getPropertiesRootNode()
                .findFirstChild(node -> isName(id, node))
                .flatMap(Node::getValueOptional)
                .orElseGet(() -> rootNodes.getAnnotationsRootNode().findFirstChild(node -> isName(id, node))
                        .flatMap(Node::getValueOptional).orElse(null));
        return Optional.ofNullable(rateContext);
    }

    private <T> boolean isName(String id, Node<T> node) {
        return id.equals(node.getName());
    }

    private Node<RateContext<K>> toRateContextNode(
            Node<RateContext<K>> parent,
            Node<RateConfig> node) {
        // Child nodes are automatically added to the specified parent.
        return Node.of(node.getName(), toRateContext(node), parent);
    }

    private RateContext<K> toRateContext(Node<RateConfig> node) {
        return RateContext.of(context.getMatcherProvider(), node);
    }

    private Optional<Node<RateConfig>> createNode(
            GenericDeclaration source, Node<RateConfig> parent) {
        return createRateConfig(source, parent == null ? null : parent.requireValue())
                .map(rateConfig -> Node.of(rateConfig.getId(), rateConfig, parent));
    }

    private Optional<RateConfig> createRateConfig(GenericDeclaration source, RateConfig parent) {
        RateSource rateSource = JavaRateSource.of(source);
        if (!rateSource.isRateLimited()) {
            return Optional.empty();
        }
        Rates rates = annotationConverter.convert(source);
        return Optional.of(RateConfig.of(rateSource, rates, parent));
    }
}
