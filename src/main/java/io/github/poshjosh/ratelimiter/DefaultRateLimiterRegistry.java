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
    public RateLimiter getRateLimiterOrUnlimited(K key) {
        final RateLimiter rateLimiter = getRateLimiterOrNull(key);
        return rateLimiter == null ? RateLimiter.NO_LIMIT : rateLimiter;
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
        return rootNodes.getPropertiesRootNode().findFirstChild(node -> isName(id, node)).isPresent()
                || rootNodes.getAnnotationsRootNode().findFirstChild(node -> isName(id, node)).isPresent();
    }

    @Override
    public boolean hasMatcher(String id) {
        return getRateContext(id).filter(RateContext::hasMatcher).isPresent();
    }

    private RateLimiter getRateLimiterOrNull(K key) {
        final RateLimiter fromCache = getRateLimiterFromCacheOrNull(key);
        if (fromCache != null) {
            return fromCache;
        }
        if (!context.isRateLimited()) {
            return null;
        }
        if (!rootNodes.hasProperties() && !rootNodes.hasAnnotations()) {
            return null;
        }

        final RateLimiter rateLimiter;
        if (!rootNodes.hasProperties()) {
            rateLimiter = createAnnotationsRateLimiter(key);
        } else if (!rootNodes.hasAnnotations()) {
            rateLimiter = createPropertisRateLimiter(key);
        } else {
            // Properties take precedence over annotations
            rateLimiter = RateLimiters.of(
                    createPropertisRateLimiter(key), createAnnotationsRateLimiter(key));
        }
        return addRateLimiterToCache(key, rateLimiter);
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
        final RateLimiter fromCache = getRateLimiterFromCacheOrNull(source);
        if (fromCache != null) {
            return fromCache;
        }
        final String rateId = RateId.of(source);
        final RateContext<K> rateContext = getRateContext(rateId)
                .orElseGet(() -> addRateContextToAnnotationsRoot(source).orElse(null));
        if (rateContext == null) {
            return null;
        }
        final RateLimiter rateLimiter = getRateLimiterOrNull(rateId, rateContext);
        return addRateLimiterToCache(source, rateLimiter);
    }

    private RateLimiter getRateLimiterOrNull(String key, RateContext<K> rateContext) {
        if (!rateContext.getSource().isRateLimited()) {
            return null;
        }
        final Rates rates = rateContext.getRatesWithParentRatesAsFallback();
        return context.getRateLimiterProvider().getRateLimiter(key, rates);
    }

    private Map<Object, RateLimiter> _keyToRateLimiterMap;
    private RateLimiter getRateLimiterFromCacheOrNull(Object key) {
        return _keyToRateLimiterMap == null ? null : _keyToRateLimiterMap.get(key);
    }
    private RateLimiter addRateLimiterToCache(Object key, RateLimiter rateLimiter) {
        if (rateLimiter == null) {
            return null;
        }
        if (_keyToRateLimiterMap == null) {
            _keyToRateLimiterMap = new WeakHashMap<>();
        }
        _keyToRateLimiterMap.put(key, rateLimiter);
        return rateLimiter;
    }

    private Optional<RateContext<K>> addRateContextToAnnotationsRoot(GenericDeclaration source) {
        Node<RateContext<K>> parent = rootNodes.getAnnotationsRootNode();
        return createNode(source)
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

    private Optional<Node<RateConfig>> createNode(GenericDeclaration source) {
        return createRateConfig(source, null)
                .map(rateConfig -> Node.of(rateConfig.getId(), rateConfig, null));
    }

    private Optional<RateConfig> createRateConfig(GenericDeclaration source, RateConfig parent) {
        RateSource rateSource = JavaRateSource.of(source);
        if (!rateSource.isRateLimited()) {
            return Optional.empty();
        }
        Rates rates = annotationConverter.convert(rateSource);
        return Optional.of(RateConfig.of(rateSource, rates, parent));
    }
}
