package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.ResourceLimiterComposition;
import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.node.Node;

import java.util.Objects;

public final class ResourceLimiterFactory<R> {

    public static <R> ResourceLimiterFactory<R> ofDefaults() {
        return new ResourceLimiterFactory<>(
                AnnotationProcessor.ofDefaults(),
                ResourceLimiterComposition.MatcherProvider.ofDefaults(),
                ResourceLimiterComposition.LimiterProvider.ofDefaults()
        );
    }

    private AnnotationProcessor<Class<?>> annotationProcessor;
    private ResourceLimiterComposition.MatcherProvider<R> matcherProvider;
    private ResourceLimiterComposition.LimiterProvider limiterProvider;

    private ResourceLimiterFactory(
            AnnotationProcessor<Class<?>> annotationProcessor,
            ResourceLimiterComposition.MatcherProvider<R> matcherProvider,
            ResourceLimiterComposition.LimiterProvider limiterProvider) {
        this.annotationProcessor = Objects.requireNonNull(annotationProcessor);
        this.matcherProvider = Objects.requireNonNull(matcherProvider);
        this.limiterProvider = Objects.requireNonNull(limiterProvider);
    }

    public ResourceLimiter<R> create(Class<?>... sources) {

        Node<RateConfig> rootNode = processAll(sources);

        return ResourceLimiterComposition.ofAnnotations(matcherProvider, limiterProvider, rootNode);
    }

    private Node<RateConfig> processAll(Class<?>... sources) {
        Node<RateConfig> rootNode = Node.of("root");
        rootNode = annotationProcessor.processAll(rootNode, sources);
        return rootNode;
    }

    public ResourceLimiterFactory<R> annotationProcessor(AnnotationProcessor<Class<?>> annotationProcessor) {
        this.annotationProcessor = annotationProcessor;
        return this;
    }

    public ResourceLimiterFactory<R> matcherProvider(ResourceLimiterComposition.MatcherProvider<R> matcherProvider) {
        this.matcherProvider = matcherProvider;
        return this;
    }

    public ResourceLimiterFactory<R> limiterProvider(ResourceLimiterComposition.LimiterProvider limiterProvider) {
        this.limiterProvider = limiterProvider;
        return this;
    }
}
