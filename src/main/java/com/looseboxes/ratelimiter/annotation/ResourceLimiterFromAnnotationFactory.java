package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;

import java.util.function.BiFunction;

public final class ResourceLimiterFromAnnotationFactory<K, V> {

    public static <K, V> ResourceLimiterFromAnnotationFactory<K, V> of() {
        return new ResourceLimiterFromAnnotationFactory<>(
                AnnotationProcessor.ofRates(), ResourceLimiterConfig.of(), (nodeName, nodeValue) -> Matcher.identity()
        );
    }

    private AnnotationProcessor<Class<?>, Rates> annotationProcessor;
    private ResourceLimiterConfig<K, V> resourceLimiterConfig;
    private PatternMatchingResourceLimiter.MatcherProvider<K> matcherProvider;

    private ResourceLimiterFromAnnotationFactory(
            AnnotationProcessor<Class<?>, Rates> annotationProcessor,
            ResourceLimiterConfig<K, V> resourceLimiterConfig,
            PatternMatchingResourceLimiter.MatcherProvider<K> matcherProvider) {
        this.annotationProcessor = annotationProcessor;
        this.resourceLimiterConfig = resourceLimiterConfig;
        this.matcherProvider = matcherProvider;
    }

    public ResourceLimiter<K> create(Class<?>... sources) {
        return new PatternMatchingResourceLimiter<K>(matcherProvider, (Node)createNode(sources), false);
    }

    public Node<NodeValue<ResourceLimiter<K>>> createNode(Class<?>... sources) {

        Node<NodeValue<Rates>> rootNode = Node.of("root");

        annotationProcessor.processAll(rootNode, sources);

        BiFunction<String, NodeValue<Rates>, NodeValue<ResourceLimiter<K>>> transformer = (nodeName, nodeValue) -> {
            Bandwidths bandwidths = RateToBandwidthConverter.of().convert(nodeValue.getValue());
            return nodeValue.withValue(ResourceLimiter.of(resourceLimiterConfig, bandwidths));
        };

        return rootNode.transform(transformer);
    }

    public ResourceLimiterFromAnnotationFactory<K, V> annotationProcessor(AnnotationProcessor<Class<?>, Rates> annotationProcessor) {
        this.annotationProcessor = annotationProcessor;
        return this;
    }

    public ResourceLimiterFromAnnotationFactory<K, V> rateLimiterConfig(ResourceLimiterConfig<K, V> resourceLimiterConfig) {
        this.resourceLimiterConfig = resourceLimiterConfig;
        return this;
    }

    public ResourceLimiterFromAnnotationFactory<K, V> matcherProvider(PatternMatchingResourceLimiter.MatcherProvider<K> matcherProvider) {
        this.matcherProvider = matcherProvider;
        return this;
    }
}
