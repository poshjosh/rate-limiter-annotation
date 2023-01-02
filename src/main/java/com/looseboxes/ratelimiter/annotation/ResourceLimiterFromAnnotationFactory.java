package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Matcher;
import com.looseboxes.ratelimiter.util.Rates;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class ResourceLimiterFromAnnotationFactory<K, V> {

    public static <K, V> ResourceLimiterFromAnnotationFactory<K, V> ofDefaults() {
        return new ResourceLimiterFromAnnotationFactory<>(
                AnnotationProcessor.ofRates(), (node) -> Matcher.identity()
        );
    }

    private AnnotationProcessor<Class<?>, Rates> annotationProcessor;
    private PatternMatchingResourceLimiter.MatcherProvider<Rates, K> matcherProvider;

    private ResourceLimiterFromAnnotationFactory(
            AnnotationProcessor<Class<?>, Rates> annotationProcessor,
            PatternMatchingResourceLimiter.MatcherProvider<Rates, K> matcherProvider) {
        this.annotationProcessor = annotationProcessor;
        this.matcherProvider = matcherProvider;
    }

    public ResourceLimiter<K> create(Class<?>... sources) {

        Node<NodeValue<Rates>> rootNode = processAll(sources);

        Map<String, ResourceLimiter<K>> limitersMap = new HashMap<>();
        rootNode.visitAll(node -> limitersMap.put(node.getName(), createResourceLimiter(node)));

        PatternMatchingResourceLimiter.LimiterProvider<Rates> limiterProvider =
                node -> limitersMap.getOrDefault(node.getName(), ResourceLimiter.noop());

        return new PatternMatchingResourceLimiter<>(
                matcherProvider, limiterProvider, rootNode, false);
    }

    public Node<NodeValue<ResourceLimiter<K>>> createNode(Class<?>... sources) {
        Function<Node<NodeValue<Rates>>, NodeValue<ResourceLimiter<K>>> transformer =
                node -> {
                    return node.getValueOptional()
                            .map(nodeValue -> nodeValue.withValue(createResourceLimiter(node)))
                            .orElse(null);
                };
        return processAll(sources).transform(transformer);
    }

    private ResourceLimiter<K> createResourceLimiter(Node<NodeValue<Rates>> node) {
        Bandwidths bandwidths = RateToBandwidthConverter.ofDefaults()
                .convert(node.getValueOptional().orElseThrow(NullPointerException::new).getValue());

        KeyProvider<K, Object> keyProvider = resource ->
                matcherProvider.getMatcher(node).matchOrNull(resource);

        return ResourceLimiter.<K>of(bandwidths).keyProvider(keyProvider);
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

    public ResourceLimiterFromAnnotationFactory<K, V> matcherProvider(PatternMatchingResourceLimiter.MatcherProvider<Rates, K> matcherProvider) {
        this.matcherProvider = matcherProvider;
        return this;
    }
}
