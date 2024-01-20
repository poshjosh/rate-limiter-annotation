package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.AnnotationConverter;
import io.github.poshjosh.ratelimiter.annotation.RateId;
import io.github.poshjosh.ratelimiter.annotation.JavaRateSource;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;

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
        if (isRegistered(RateId.of(source))) {
            return this;
        }
        Node<RateConfig> node = createNode(source);
        toRateContextNode(rootNodes.getAnnotationsRootNode(), node);
        return this;
    }

    @Override
    public RateLimiterRegistry<K> register(Method source) {
        if (isRegistered(RateId.of(source))) {
            return this;
        }
        Node<RateConfig> node = createNode(source);
        toRateContextNode(rootNodes.getAnnotationsRootNode(), node);
        return this;
    }

    @Override
    public RateLimiterFactory<K> createRateLimiterFactory() {
        return RateLimiterFactoryCreator.create(context, rootNodes);
    }

    @Override
    public Optional<RateLimiter> getRateLimiter(Class<?> clazz) {
        RateContext<K> context = getRateContext(clazz).orElseGet(() -> createRateContext(clazz));
        return getRateLimiter(RateId.of(clazz), context.getRates());
    }

    @Override
    public Optional<RateLimiter> getRateLimiter(Method method) {
        RateContext<K> context = getRateContext(method).orElseGet(() -> createRateContext(method));
        return getRateLimiter(RateId.of(method), context.getRates());
    }

    @Override
    public boolean isRegistered(String id) {
        return rootNodes.getPropertiesRootNode().findFirstChild(node -> isName(id, node)).isPresent()
                || rootNodes.getAnnotationsRootNode().findFirstChild(node -> isName(id, node)).isPresent();
    }

    private Optional<RateLimiter> getRateLimiter(String key, Rates rates) {
        if (!rates.hasLimits()) {
            return Optional.empty();
        }
        RateLimiterProvider provider = context.getRateLimiterProvider();
        return Optional.of(provider.getRateLimiter(key, rates));
    }

    private RateContext<K> createRateContext(Class<?> source) {
        Node<RateConfig> node = createNode(source);
        return toRateContextNode(null, node).requireValue();
    }
    private RateContext<K> createRateContext(Method source) {
        Node<RateConfig> node = createNode(source);
        return toRateContextNode(null, node).requireValue();
    }
    private Optional<RateContext<K>> getRateContext(Class<?> clazz) {
        return getRateContext(RateId.of(clazz));
    }
    private Optional<RateContext<K>> getRateContext(Method method) {
        return getRateContext(RateId.of(method));
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

    private Node<RateConfig> createNode( Class<?> source) {
        RateConfig rateConfig = createRateConfig(source);
        return Node.of(rateConfig.getId(), rateConfig, null);
    }

    private Node<RateConfig> createNode(Method source) {
        RateConfig rateConfig = createRateConfig(source);
        return Node.of(rateConfig.getId(), rateConfig, null);
    }

    private RateConfig createRateConfig(Class<?> source) {
        Rates rates = annotationConverter.convert(source);
        return RateConfig.of(JavaRateSource.of(source), rates, null);
    }

    private RateConfig createRateConfig(Method source) {
        Rates rates = annotationConverter.convert(source);
        return RateConfig.of(JavaRateSource.of(source), rates, null);
    }
}
