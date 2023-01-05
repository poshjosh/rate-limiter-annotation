package com.looseboxes.ratelimiter.performance;

import com.looseboxes.ratelimiter.RateToBandwidthConverter;
import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.RateConfig;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.NodeFormatter;
import com.looseboxes.ratelimiter.util.ClassesInPackageFinder;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

class AnnotationProcessingPerformanceIT {

    @Test
    void annotationProcessShouldConsumeLimitedTimeAndMemory() {
        Usage bookmark = Usage.bookmark();
        Node<ResourceLimiter<Object>> rateLimiterRootNode = buildRateLimiters();
        bookmark.assertUsageLessThan(Usage.of(250, 25_000_000));
        System.out.println(NodeFormatter.indentedHeirarchy().format(rateLimiterRootNode));
    }

    Node<ResourceLimiter<Object>> buildRateLimiters() {
        List<Class<?>> classList = ClassesInPackageFinder.ofDefaults().findClasses(
                Collections.singletonList(getClass().getPackage().getName()),
                clazz -> true);
        Node<RateConfig> rootNode = Node.of("root");
        AnnotationProcessor.ofDefaults().processAll(rootNode, classList);

        Function<Node<RateConfig>, ResourceLimiter<Object>> transformer = node -> {
            return node.getValueOptional().map(nodeValue -> {
                Bandwidths bandwidths = RateToBandwidthConverter.ofDefaults().convert(nodeValue.getValue());
                return ResourceLimiter.of(bandwidths);
            }).orElse(null);
        };

        return rootNode.transform(transformer);
    }
}
