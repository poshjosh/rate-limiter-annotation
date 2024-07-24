package io.github.poshjosh.ratelimiter.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@FunctionalInterface
public interface ClassesInPackageFinder {

    @FunctionalInterface
    interface ClassFilter extends Predicate<Class<?>> { }

    static ClassesInPackageFinder ofDefaults() {
        return new DefaultClassesInPackageFinder(Thread.currentThread().getContextClassLoader());
    }

    default List<Class<?>> findClasses(String packageName) {
        return findClasses(packageName, clazz -> true);
    }

    default List<Class<?>> findClasses(List<String> packageNames) {
        return findClasses(packageNames, clazz -> true);
    }

    default List<Class<?>> findClasses(List<String> packageNames, ClassFilter classFilter) {
        Objects.requireNonNull(classFilter);
        return packageNames.isEmpty() ? Collections.emptyList() : packageNames.stream()
                .flatMap(packageName -> findClasses(packageName, classFilter).stream())
                .collect(Collectors.toList());
    }

    List<Class<?>> findClasses(String packageName, ClassFilter classFilter);
}
