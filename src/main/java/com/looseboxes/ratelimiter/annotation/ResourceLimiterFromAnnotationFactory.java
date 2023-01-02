package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class ResourceLimiterFromAnnotationFactory<K, V> {

    public static <K, V> ResourceLimiterFromAnnotationFactory<K, V> ofDefaults() {
        return new ResourceLimiterFromAnnotationFactory<>(
                AnnotationProcessor.ofRates(), ResourceLimiterConfig.ofDefaults(), (node) -> Matcher.identity()
        );
    }

    private AnnotationProcessor<Class<?>, Rates> annotationProcessor;
    private ResourceLimiterConfig<K, V> resourceLimiterConfig;
    private PatternMatchingResourceLimiter.MatcherProvider<Rates, K> matcherProvider;

    private ResourceLimiterFromAnnotationFactory(
            AnnotationProcessor<Class<?>, Rates> annotationProcessor,
            ResourceLimiterConfig<K, V> resourceLimiterConfig,
            PatternMatchingResourceLimiter.MatcherProvider<Rates, K> matcherProvider) {
        this.annotationProcessor = annotationProcessor;
        this.resourceLimiterConfig = resourceLimiterConfig;
        this.matcherProvider = matcherProvider;
    }

    public ResourceLimiter<K> create(Class<?>... sources) {

        Node<NodeValue<Rates>> rootNode = processAll(sources);

        Map<String, ResourceLimiter<K>> limitersMap = new HashMap<>();
        rootNode.visitAll(node -> node.getValueOptional()
                .map(ResourceLimiterFromAnnotationFactory.this::createResourceLimiter)
                .ifPresent(resourceLimiter -> limitersMap.put(node.getName(), resourceLimiter)));

        PatternMatchingResourceLimiter.LimiterProvider<Rates> limiterProvider =
                node -> limitersMap.getOrDefault(node.getName(), ResourceLimiter.noop());

        return new PatternMatchingResourceLimiter<>(
                matcherProvider, limiterProvider, rootNode, false);
    }

    public Node<NodeValue<ResourceLimiter<K>>> createNode(Class<?>... sources) {
        BiFunction<String, NodeValue<Rates>, NodeValue<ResourceLimiter<K>>> transformer =
                (nodeName, nodeValue) -> nodeValue.withValue(createResourceLimiter(nodeValue));
        return processAll(sources).transform(transformer);
    }

    private ResourceLimiter<K> createResourceLimiter(NodeValue<Rates> nodeValue) {
        Bandwidths bandwidths = RateToBandwidthConverter.ofDefaults().convert(nodeValue.getValue());
        return ResourceLimiter.of(resourceLimiterConfig, bandwidths);
    }

    private Node<NodeValue<Rates>> processAll(Class<?>... sources) {
        Node<NodeValue<Rates>> rootNode = Node.of("root");
        annotationProcessor.processAll(rootNode, sources);
        return rootNode;
    }

    public ResourceLimiterFromAnnotationFactory<K, V> annotationProcessor(AnnotationProcessor<Class<?>, Rates> annotationProcessor) {
        this.annotationProcessor = annotationProcessor;
        return this;
    }

    public ResourceLimiterFromAnnotationFactory<K, V> rateLimiterConfig(ResourceLimiterConfig<K, V> resourceLimiterConfig) {
        this.resourceLimiterConfig = resourceLimiterConfig;
        return this;
    }

    public ResourceLimiterFromAnnotationFactory<K, V> matcherProvider(PatternMatchingResourceLimiter.MatcherProvider<Rates, K> matcherProvider) {
        this.matcherProvider = matcherProvider;
        return this;
    }
}
