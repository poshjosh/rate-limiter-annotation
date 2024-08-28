package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.matcher.MatchContext;
import io.github.poshjosh.ratelimiter.matcher.MatchContexts;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.MutableNode;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;
import io.github.poshjosh.ratelimiter.util.Ticker;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class DefaultRateLimiterRegistry<K> implements RateLimiterRegistry<K> {

    private final RateLimiterContext<K> context;
    private final RootNodes<K> rootNodes;

    private final boolean allowRateLessSources = true;

    DefaultRateLimiterRegistry(
            RateLimiterContext<K> context,
            RootNodes<K> rootNodes) {
        this.context = Objects.requireNonNull(context);
        this.rootNodes = Objects.requireNonNull(rootNodes);
        ((MutableNode<?>)this.rootNodes.getPropertiesRootNode()).collectLeafs();
        ((MutableNode<?>)this.rootNodes.getAnnotationsRootNode()).collectLeafs();
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
            if (!rootNodes.hasAnnotations()) {
                return true;
            }
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
            if (!rootNodes.hasAnnotations()) {
                return true;
            }
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
    public Set<String> getRateNames() {
        final Set<String> names = new HashSet<>();
        rootNodes.getPropertiesRootNode().visitAll(node -> names.add(node.getName()));
        names.remove(rootNodes.getPropertiesRootNode().getName());
        rootNodes.getAnnotationsRootNode().visitAll(node -> names.add(node.getName()));
        names.remove(rootNodes.getAnnotationsRootNode().getName());
        return Collections.unmodifiableSet(names);
    }

    @Override
    public void visitRates(Consumer<MatchContext<K>> visitor) {
        rootNodes.getPropertiesRootNode().getChildren()
                .forEach(child -> child.visitAll(node -> visitor.accept(node.getValueOrDefault(null))));
        rootNodes.getAnnotationsRootNode().getChildren()
                .forEach(child -> child.visitAll(node -> visitor.accept(node.getValueOrDefault(null))));
    }

    @Override
    public MatchContext<K> getMatchContextOrDefault(String id, MatchContext<K> resultIfNone) {
        final MatchContext<K> matchContext = this.getMatchContextOrNull(id);
        return matchContext == null ? resultIfNone : matchContext;
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
            onRateRemoved(removed);
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
        add(rateSource);
        return this;
    }

    /**
     * Get a RateLimiter for the specified key.
     * A rate limiter which is a composition of multiple rate limiters is created and returned.
     * Each rate limiter in the composition is akin to those returned by the method.
     * <p>
     *     The rate limiter is not cached, so that each call to this method will return a
     *     new instance. Use for cases where this method will never be called with the
     *     same key, more than once. E.g: Where each key is a http request.
     *     If want to cache the provided rate limiter, use the factory method
     *     {@link RateLimiterRegistries#ofCaching(RateLimiterRegistry)} to wrap this registry.
     * </p>
     * @param key The key for which a rate limiter is to be returned.
     * @param resultIfNone The rate limiter to return if none is found.
     * @return The RateLimiter for the specified key or the provided default if none is found.
     * @see RateLimiterRegistries#ofCaching(RateLimiterRegistry)
     * @see #getRateLimiterOrDefault(RateSource, RateLimiter)
     */
    @Override
    public RateLimiter getRateLimiterOrDefault(K key, RateLimiter resultIfNone) {
        // This rate-limiter is not cached.
        // Each key will always lead to the construction of a new RateLimiter.
        // Use for cases where this method will never be called with the same
        // key, more than once. E.g: Where each key is a http request.
        return createRateLimiterOrDefault(key, resultIfNone);
    }

    /**
     * Get or create the rate limiter for the specified rate source.
     * A rate source may be a class, method, or any other implementation of RateSource.
     * <p>
     *     This RateLimiter is cached.
     *     Since each unique RateSource will always refer to the same RateLimiter,
     *     we cache the RateLimiter for re-use.
     * </p>
     *
     * @param rateSource The RateSource for which a rate limiter is to be returned.
     * @param resultIfNone The rate limiter to return if none is found.
     * @return The RateLimiter for the specified RateSource or the provided default if none is found.
     * @see RateSource
     * @see io.github.poshjosh.ratelimiter.model.RateSources
     * @see io.github.poshjosh.ratelimiter.annotation.JavaRateSources
     */
    @Override
    public RateLimiter getRateLimiterOrDefault(RateSource rateSource, RateLimiter resultIfNone) {
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
        final MatchContext<K> matchContext = getMatchContextOrNull(id);
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
            if (!rootNodes.hasAnnotations()) {
                return resultIfNone;
            }
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
        MatchContext<K> matchContext = getMatchContextOrNull(id);
        if (matchContext == null) {
            final Node<MatchContext<K>> added = add(rateSource);
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
        final Rates rates = matchContext.getRatesOrParentRates();
        return context.getRateLimiterProvider().getRateLimiter(key, rates);
    }

    private Node<MatchContext<K>> add(RateSource rateSource) {
        return addTo(rateSource, getParentNode(rateSource));
    }

    private Node<MatchContext<K>> getParentNode(RateSource rateSource) {
        final String parentId = rateSource.getRates().getParentId();
        if (parentId == null || parentId.isEmpty()) {
            return rateSource.isGenericDeclaration() ?
                    rootNodes.getAnnotationsRootNode() : rootNodes.getPropertiesRootNode();
        }
        final Node<MatchContext<K>> parent = this.getNodeOrNull(parentId);
        if (parent == null) {
            throw new IllegalArgumentException("Parent not found. parentId: " + parentId
                    + ", of: " + rateSource);
        }
        return parent;
    }

    private Node<MatchContext<K>> addTo(
            RateSource rateSource, Node<MatchContext<K>> parent) {
        if (!allowRateLessSources && !rateSource.isRateLimited()) {
            return null;
        }
        final RateConfig parentConfig = parent.getValueOptional()
                .map(MatchContext::getRateConfig).orElse(null);
        final Node<RateConfig> node = createNodeOrNull(rateSource, parentConfig);
        if (node == null) {
            return null;
        }
        final Node<MatchContext<K>> result = toRateContextNode(parent, node);
        onRateAdded(result);
        return result;
    }

    private void onRateAdded(Node<MatchContext<K>> node) {
        node.getValueOptional()
                .map(MatchContext::getRateConfig)
                .map(RateConfig::getId)
                .ifPresent(id -> ((MutableNode<?>)findRootNode(id)).collectLeafs());
    }

    private void onRateRemoved(Node<MatchContext<K>> node) {
        node.getValueOptional()
                .map(MatchContext::getRateConfig)
                .map(RateConfig::getParent)
                // Use parent.id here because the rateConfig is already removed
                .map(RateConfig::getId)
                .ifPresent(parentId -> ((MutableNode<?>)findRootNode(parentId)).collectLeafs());
    }

    private MatchContext<K> getMatchContextOrNull(String id) {
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
        if (!allowRateLessSources && !rateSource.isRateLimited()) {
            return null;
        }
        return Nodes.of(rateSource.getId(),
                RateConfig.of(rateSource, rateSource.getRates(), parent));
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(1024);
        return builder.append(getClass().getName())
                .append('@')
                .append(Integer.toHexString(System.identityHashCode(this)))
                .append('{')
                .append("\nAnnotation sourced nodes:\n").append(rootNodes.getAnnotationsRootNode())
                .append("\nProperties sourced nodes:\n").append(rootNodes.getPropertiesRootNode())
                .append("\nContext:").append(context)
                .append('}').toString();
    }
}
