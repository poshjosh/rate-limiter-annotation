package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.*;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Matcher;

import java.util.HashMap;
import java.util.Map;

public final class ResourceLimiterFactory<R> {

    public static <R> ResourceLimiterFactory<R> ofDefaults() {
        return new ResourceLimiterFactory<>(
                AnnotationProcessor.ofDefaults(), (node) -> Matcher.identity()
        );
    }

    private AnnotationProcessor<Class<?>> annotationProcessor;
    private MatchedResourceLimiter.MatcherProvider<R> matcherProvider;

    private ResourceLimiterFactory(
            AnnotationProcessor<Class<?>> annotationProcessor,
            MatchedResourceLimiter.MatcherProvider<R> matcherProvider) {
        this.annotationProcessor = annotationProcessor;
        this.matcherProvider = matcherProvider;
    }

    public ResourceLimiter<R> create(Class<?>... sources) {

        Node<RateConfig> rootNode = processAll(sources);

        Map<String, ResourceLimiter<R>> limitersMap = new HashMap<>();
        rootNode.visitAll(node -> limitersMap.put(node.getName(), createResourceLimiter(node)));

        MatchedResourceLimiter.LimiterProvider limiterProvider =
                node -> limitersMap.getOrDefault(node.getName(), ResourceLimiter.noop());

        return MatchedResourceLimiter.ofAnnotations(matcherProvider, limiterProvider, rootNode);
    }

    private ResourceLimiter<R> createResourceLimiter(Node<RateConfig> node) {
        Bandwidths bandwidths = RateToBandwidthConverter.ofDefaults()
                .convert(node.getValueOptional().orElseThrow(NullPointerException::new).getValue());
        return ResourceLimiter.<R>of(bandwidths);
    }

    private Node<RateConfig> processAll(Class<?>... sources) {
        Node<RateConfig> rootNode = Node.of("root");
        annotationProcessor.processAll(rootNode, sources);
        return rootNode;
    }

    public ResourceLimiterFactory<R> annotationProcessor(AnnotationProcessor<Class<?>> annotationProcessor) {
        this.annotationProcessor = annotationProcessor;
        return this;
    }

    public ResourceLimiterFactory<R> matcherProvider(MatchedResourceLimiter.MatcherProvider<R> matcherProvider) {
        this.matcherProvider = matcherProvider;
        return this;
    }
}
