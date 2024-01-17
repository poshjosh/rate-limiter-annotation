package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.AnnotationConverter;
import io.github.poshjosh.ratelimiter.annotation.ElementId;
import io.github.poshjosh.ratelimiter.annotation.JavaRateSource;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

final class DefaultRateLimiterRegistry<K> implements RateLimiterRegistry<K> {

    private final RateLimiterContext<K> context;
    private final RootNodes<K> rootNodes;
    private final AnnotationConverter<Rate, Rates> annotationConverter;

    DefaultRateLimiterRegistry(
            RateLimiterContext<K> context,
            RootNodes<K> rootNodes,
            AnnotationConverter<Rate, Rates> annotationConverter) {
        this.context = Objects.requireNonNull(context);
        this.annotationConverter = Objects.requireNonNull(annotationConverter);
        this.rootNodes = Objects.requireNonNull(rootNodes);
    }

    @Override
    public RateLimiterRegistry<K> register(Class<?> source) {
        if (isRegistered(ElementId.of(source))) {
            return this;
        }
        Node<RateConfig> node = createNode(source);
        toLimiterContextNode(rootNodes.getAnnotationsRootNode(), node);
        return this;
    }

    @Override
    public RateLimiterRegistry<K> register(Method source) {
        if (isRegistered(ElementId.of(source))) {
            return this;
        }
        Node<RateConfig> node = createNode(source);
        toLimiterContextNode(rootNodes.getAnnotationsRootNode(), node);
        return this;
    }

    @Override
    public RateLimiterFactory<K> createRateLimiterFactory() {
        return RateLimiterFactoryCreator.create(context, rootNodes);
    }

    @Override
    public Optional<RateLimiter> getRateLimiter(Class<?> clazz) {
        LimiterContext<K> context = getLimiterContext(clazz).orElseGet(() -> createLimiterContext(clazz));
        return getRateLimiter(ElementId.of(clazz), context.getRates());
    }

    @Override
    public Optional<RateLimiter> getRateLimiter(Method method) {
        LimiterContext<K> context = getLimiterContext(method).orElseGet(() -> createLimiterContext(method));
        return getRateLimiter(ElementId.of(method), context.getRates());
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

    private LimiterContext<K> createLimiterContext(Class<?> source) {
        Node<RateConfig> node = createNode(source);
        return toLimiterContextNode(null, node).requireValue();
    }
    private LimiterContext<K> createLimiterContext(Method source) {
        Node<RateConfig> node = createNode(source);
        return toLimiterContextNode(null, node).requireValue();
    }
    private Optional<LimiterContext<K>> getLimiterContext(Class<?> clazz) {
        return getLimiterContext(ElementId.of(clazz));
    }
    private Optional<LimiterContext<K>> getLimiterContext(Method method) {
        return getLimiterContext(ElementId.of(method));
    }
    private Optional<LimiterContext<K>> getLimiterContext(String id) {
        LimiterContext<K> limiterContext = rootNodes.getPropertiesRootNode()
                .findFirstChild(node -> isName(id, node))
                .flatMap(Node::getValueOptional)
                .orElseGet(() -> rootNodes.getAnnotationsRootNode().findFirstChild(node -> isName(id, node))
                        .flatMap(Node::getValueOptional).orElse(null));
        return Optional.ofNullable(limiterContext);
    }

    private <T> boolean isName(String id, Node<T> node) {
        return id.equals(node.getName());
    }

    private Node<LimiterContext<K>> toLimiterContextNode(
            Node<LimiterContext<K>> parent,
            Node<RateConfig> node) {
        // Child nodes are automatically added to the specified parent.
        return Node.of(node.getName(), toLimiterContext(node), parent);
    }

    private LimiterContext<K> toLimiterContext(Node<RateConfig> node) {
        return LimiterContext.of(context.getMatcherProvider(), node);
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
        return RateConfig.of(JavaRateSource.of(source), rates);
    }

    private RateConfig createRateConfig(Method source) {
        Rates rates = annotationConverter.convert(source);
        return RateConfig.of(JavaRateSource.of(source), rates);
    }
}
