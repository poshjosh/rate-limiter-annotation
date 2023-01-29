package io.github.poshjosh.ratelimiter.performance;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AnnotationProcessingPerformanceIT {

    @Test
    void annotationProcessShouldConsumeLimitedTimeAndMemory() {

        // This package contains 100 randomly rate limited classes
        final String packageName = "io.github.poshjosh.ratelimiter.performance.dummyclasses";

        Usage bookmark = Usage.bookmark();
        List<Class<?>> classList = ClassesInPackageFinder.ofDefaults()
                .findClasses(Collections.singletonList(packageName), clazz -> true);
        assertFalse(classList.isEmpty());

        Node<RateConfig> rootNode = RateProcessor.ofDefaults().processAll(new HashSet<>(classList));

        ResourceLimiter.of(rootNode);

        final int size = classList.size();

        bookmark.assertUsageLessThan(Usage.of(size * 4, size * 350_000));

        // This should come after recording usage
        //System.out.println(rootNode);
    }
}
