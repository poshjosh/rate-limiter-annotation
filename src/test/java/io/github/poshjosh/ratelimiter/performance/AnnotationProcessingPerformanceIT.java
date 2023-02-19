package io.github.poshjosh.ratelimiter.performance;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
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

        ResourceLimiter.of(classList.toArray(new Class[0]));

        final int size = classList.size();

        bookmark.assertUsageLessThan(Usage.of(size * 4, size * 350_000));
    }
}
