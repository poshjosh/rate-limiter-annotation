package com.looseboxes.ratelimiter.performance;

import com.looseboxes.ratelimiter.RateToBandwidthConverter;
import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.NodeFormatter;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import com.looseboxes.ratelimiter.util.Rates;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

class AnnotationProcessingPerformanceIT {

    @Test
    void annotationProcessShouldConsumeLimitedTimeAndMemory() {
        Usage bookmark = Usage.bookmark();
        Node<NodeValue<ResourceLimiter<Object>>> rateLimiterRootNode = buildRateLimiters();
        bookmark.assertUsageLessThan(Usage.of(250, 25_000_000));
        System.out.println(NodeFormatter.indentedHeirarchy().format(rateLimiterRootNode));
    }

    Node<NodeValue<ResourceLimiter<Object>>> buildRateLimiters() {
        List<Class<?>> classList = ClassesInPackageFinder.ofDefaults().findClasses(
                Collections.singletonList(getClass().getPackage().getName()),
                clazz -> true);
        Node<NodeValue<Rates>> rootNode = Node.of("root");
        AnnotationProcessor.ofRates().processAll(rootNode, classList);

        Function<Node<NodeValue<Rates>>, NodeValue<ResourceLimiter<Object>>> transformer = node -> {
            return node.getValueOptional().map(nodeValue -> {
                Bandwidths bandwidths = RateToBandwidthConverter.ofDefaults().convert(nodeValue.getValue());
                return nodeValue.withValue(ResourceLimiter.of(bandwidths));
            }).orElse(null);
        };

        return rootNode.transform(transformer);
    }
}
