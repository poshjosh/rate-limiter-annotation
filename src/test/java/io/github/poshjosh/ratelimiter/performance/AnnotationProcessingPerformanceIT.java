package io.github.poshjosh.ratelimiter.performance;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationProcessingPerformanceIT {

    @Test
    void annotationProcessShouldConsumeLimitedTimeAndMemory() {

        // Do not use log level debug/trace, as tests may fail
        Usage usageBookmark = Usage.bookmark();

        // This package contains 100 randomly rate limited classes
        final String packageName = "io.github.poshjosh.ratelimiter.performance.dummyclasses";

        final List<Class<?>> classList = ClassesInPackageFinder.ofDefaults()
                .findClasses(Collections.singletonList(packageName), clazz -> true);
        assertFalse(classList.isEmpty());

        ResourceLimiter.of(classList.toArray(new Class[0]));

        final int size = classList.size();

        Usage currentUsage = usageBookmark.current();
        System.out.println(currentUsage);
        Usage usageLimit = Usage.of(size * 4, size * 350_000);
        assertFalse(currentUsage.isAnyUsageGreaterThan(usageLimit),
                "Usage should be less or equal to limit, but was not.\nUsage: " +
                        currentUsage + "\nLimit: " + usageLimit);
    }
}
