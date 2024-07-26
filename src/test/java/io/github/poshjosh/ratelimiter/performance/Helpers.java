package io.github.poshjosh.ratelimiter.performance;

import io.github.poshjosh.ratelimiter.RateLimiterRegistries;
import io.github.poshjosh.ratelimiter.RateLimiterRegistry;
import io.github.poshjosh.ratelimiter.util.ClassesInPackageFinder;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class Helpers {

    static RateLimiterRegistry givenRateLimiterRegistry() {
        return givenRateLimiterRegistry(annotatedClasses());
    }

    static RateLimiterRegistry givenRateLimiterRegistry(List<Class<?>> classList) {
        final RateLimiterRegistry<?> registry = RateLimiterRegistries.of(classList.toArray(new Class[0]));
        // Caching should make tests faster
        return RateLimiterRegistries.ofCaching(registry);
    }

    static List<Method> annotatedClassMethods() {
        return annotatedClasses().stream()
                .map(Class::getDeclaredMethods)
                .flatMap(Stream::of).collect(Collectors.toList());
    }

    static List<Class<?>> annotatedClasses() {
        // This package contains 100 randomly rate limited classes
        final String packageName = "io.github.poshjosh.ratelimiter.performance.dummyclasses";

        final List<Class<?>> classList = ClassesInPackageFinder.ofDefaults()
                .findClasses(Collections.singletonList(packageName), clazz -> true);
        assertFalse(classList.isEmpty());
        return classList;
    }
}
