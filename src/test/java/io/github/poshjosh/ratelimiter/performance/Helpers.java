package io.github.poshjosh.ratelimiter.performance;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class Helpers {

    static List<Class<?>> annotatedClasses() {
        // This package contains 100 randomly rate limited classes
        final String packageName = "io.github.poshjosh.ratelimiter.performance.dummyclasses";

        final List<Class<?>> classList = ClassesInPackageFinder.ofDefaults()
                .findClasses(Collections.singletonList(packageName), clazz -> true);
        assertFalse(classList.isEmpty());
        return classList;
    }

    static ResourceLimiter givenResourceLimiterFrom(List<Class<?>> classList) {
        return ResourceLimiter.of(classList.toArray(new Class[0]));
    }
}
