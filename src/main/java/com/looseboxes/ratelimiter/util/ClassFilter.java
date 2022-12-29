package com.looseboxes.ratelimiter.util;

import java.util.function.Predicate;

@FunctionalInterface
public interface ClassFilter extends Predicate<Class<?>> {
}
