package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.node.Node;

final class RateLimiterFactoryCreator {
    private RateLimiterFactoryCreator() { }

    static <K> RateLimiterFactory<K> create(RateLimiterContext<K> context) {
        if (!context.isRateLimited()) {
            return RateLimiterFactory.noop();
        }
        return create(context, RootNodes.of(context));
    }

    static <K> RateLimiterFactory<K> create(RateLimiterContext<K> context, RootNodes<K> rootNodes) {
        if (!context.isRateLimited()) {
            return RateLimiterFactory.noop();
        }
        Node<LimiterContext<K>> propsRoot = rootNodes.getPropertiesRootNode();
        Node<LimiterContext<K>> annoRoot = rootNodes.getAnnotationsRootNode();
        RateLimiterProvider provider = context.getRateLimiterProvider();
        if (propsRoot.isEmptyNode() || propsRoot.size() == 0) {
            return new DefaultRateLimiterFactory<>(annoRoot, provider);
        }

        if (annoRoot.isEmptyNode() || annoRoot.size() == 0) {
            return new DefaultRateLimiterFactory<>(propsRoot, provider);
        }

        RateLimiterFactory<K> propsFactory = new DefaultRateLimiterFactory<>(propsRoot, provider);
        RateLimiterFactory<K> annoFactory = new DefaultRateLimiterFactory<>(annoRoot, provider);

        // Properties take precedence over annotations
        return propsFactory.andThen(annoFactory);
    }
}
