package io.github.poshjosh.ratelimiter;

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
        RateLimiterFactory<K> propsFactory = new DefaultRateLimiterFactory<>(
                rootNodes.getPropertiesRootNode(), context.getRateLimiterProvider());
        RateLimiterFactory<K> annoFactory = new DefaultRateLimiterFactory<>(
                rootNodes.getAnnotationsRootNode(), context.getRateLimiterProvider());

        // Properties take precedence over annotations
        return propsFactory.andThen(annoFactory);
    }
}
