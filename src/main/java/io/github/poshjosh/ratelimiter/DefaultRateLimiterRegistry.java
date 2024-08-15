package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.MutableNode;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;
import io.github.poshjosh.ratelimiter.util.Ticker;

import java.util.*;
import java.util.concurrent.TimeUnit;

final class DefaultRateLimiterRegistry<K>
        implements RateLimiterRegistry<K>, RateLimiterRegistry.Listener {

    private final RateLimiterContext<K> context;
    private final RootNodes<K> rootNodes;

    private final List<RateLimiterRegistry.Listener> listeners;

    DefaultRateLimiterRegistry(
            RateLimiterContext<K> context,
            RootNodes<K> rootNodes) {
        this.context = Objects.requireNonNull(context);
        this.rootNodes = Objects.requireNonNull(rootNodes);
        ((MutableNode<?>)this.rootNodes.getPropertiesRootNode()).collectLeafs();
        ((MutableNode<?>)this.rootNodes.getAnnotationsRootNode()).collectLeafs();
        this.listeners = new ArrayList<>();
        addListener(this);
    }

    @Override
    public void onRateAdded(RateConfig rateConfig) {
        ((MutableNode<?>)findRootNode(rateConfig.getId())).collectLeafs();
    }

    @Override
    public void onRateRemoved(RateConfig rateConfig) {
        // Use parent.id here because the rateConfig is already removed
        final String parentId = rateConfig.getParent().getId();
        ((MutableNode<?>)findRootNode(parentId)).collectLeafs();
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
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

    public RateLimiterRegistry<K> deregister(String id) {
        Node<MatchContext<K>> node = this.getNodeOrNull(id);
        if (node == null) {
            return this;
        }
        Node<MatchContext<K>> parentNode = node.getParentOrDefault(null);
        if (parentNode == null) {
            throw new UnsupportedOperationException("Cannot deregister root node");
        }
        if (parentNode instanceof MutableNode) {
            Node<MatchContext<K>> removed =
                    ((MutableNode<MatchContext<K>>)parentNode).removeChild(node.getName());
            RateConfig rateConfig =
                    removed.getValueOptional().map(MatchContext::getRateConfig).orElse(null);
            listeners.forEach(listener -> listener.onRateRemoved(rateConfig));
            return this;
        }
        throw new UnsupportedOperationException("Cannot deregister node from immutable parent");
    }

    @Override
    public RateLimiterRegistry<K> register(RateSource rateSource) {
        final String id = rateSource.getId();
        if (isRegistered(id)) {
            complainAlreadyRegistered(id);
        }
        addToRoot(rateSource);
        return this;
    }

    @Override
    public RateLimiter getRateLimiterOrDefault(K key, RateLimiter resultIfNone) {
        // This rate limiter is not cached.
        // Uses for cases where each key is unique. E.g: http request.
        // Since each http request is unique, no need to cache the rate limiter for re-use.
        return createRateLimiterOrDefault(key, resultIfNone);
    }

    @Override
    public RateLimiter getRateLimiterOrDefault(RateSource rateSource, RateLimiter resultIfNone) {
        // This rate-limiter is cached.
        return getSourceRateLimiterOr(rateSource, resultIfNone);
    }

    @Override
    public boolean isRegistered(String id) {
        final Object found = rootNodes.getPropertiesRootNode()
                .findFirstOrDefault(node -> isName(id, node), null);
        if (found != null) {
            return true;
        }
        return rootNodes.getAnnotationsRootNode()
                .findFirstOrDefault(node -> isName(id, node), null) != null;
    }

    @Override
    public boolean hasMatcher(String id) {
        final MatchContext<K> matchContext = getRateContextOrNull(id);
        return matchContext != null && matchContext.hasMatcher();
    }

    private void complainAlreadyRegistered(String id) {
        throw new UnsupportedOperationException("Already registered: " + id);
    }

    private Node<MatchContext<K>> findRootNode(String id) {
        Object found = rootNodes.getPropertiesRootNode()
                .findFirstOrDefault(node -> isName(id, node), null);
        if (found != null) {
            return rootNodes.getPropertiesRootNode();
        }
        found = rootNodes.getAnnotationsRootNode()
                .findFirstOrDefault(node -> isName(id, node), null);
        Objects.requireNonNull(found);
        return rootNodes.getAnnotationsRootNode();
    }

    private RateLimiter createRateLimiterOrDefault(K key, RateLimiter resultIfNone) {
        if (!isRateLimitingSetup()) {
            return resultIfNone;
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

    private RateLimiter getSourceRateLimiterOr(RateSource rateSource, RateLimiter resultIfNone) {
        final String id = rateSource.getId();
        MatchContext<K> matchContext = getRateContextOrNull(id);
        if (matchContext == null) {
            final Node<MatchContext<K>> added = addToRoot(rateSource);
            matchContext = added == null ? null : added.requireValue();
        }
        if (matchContext == null) {
            return resultIfNone;
        }
        return getRateLimiterOrDefault(id, matchContext, resultIfNone);
    }

    private RateLimiter getRateLimiterOrDefault(
            String key, MatchContext<K> matchContext, RateLimiter resultIfNone) {
        if (!matchContext.getSource().isRateLimited()) {
            return resultIfNone;
        }
        final Rates rates = matchContext.getRatesWithParentRatesAsFallback();
        return context.getRateLimiterProvider().getRateLimiter(key, rates);
    }

    private Node<MatchContext<K>> addToRoot(RateSource rateSource) {
        final Node<MatchContext<K>> parent = rateSource.isGenericDeclaration() ?
                rootNodes.getAnnotationsRootNode() : rootNodes.getPropertiesRootNode();
        return addTo(rateSource, parent);
    }

    private Node<MatchContext<K>> addTo(
            RateSource rateSource, Node<MatchContext<K>> parent) {
        if (!rateSource.isRateLimited()) {
            return null;
        }
        final RateConfig parentConfig = parent.getValueOptional()
                .map(MatchContext::getRateConfig).orElse(null);
        final Node<RateConfig> node = createNodeOrNull(rateSource, parentConfig);
        if (node == null) {
            return null;
        }
        final RateConfig rateConfig = node.requireValue();
        final Node<MatchContext<K>> result = toRateContextNode(parent, node);
        listeners.forEach(listener -> listener.onRateAdded(rateConfig));
        return result;
    }

    private MatchContext<K> getRateContextOrNull(String id) {
        Node<MatchContext<K>> node = getNodeOrNull(id);
        return node == null ? null : node.getValueOrDefault(null);
    }
    private Node<MatchContext<K>> getNodeOrNull(String id) {
        Node<MatchContext<K>> result = rootNodes.getPropertiesRootNode()
                .findFirstOrDefault(node -> isName(id, node), null);
        if (result != null) {
            return result;
        }
        return rootNodes.getAnnotationsRootNode()
                .findFirstOrDefault(node -> isName(id, node), null);
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

    private Node<RateConfig> createNodeOrNull(RateSource rateSource, RateConfig parent) {
        if (!rateSource.isRateLimited()) {
            return null;
        }
        return Nodes.of(rateSource.getId(),
                RateConfig.of(rateSource, rateSource.getRates(), parent));
    }
}
