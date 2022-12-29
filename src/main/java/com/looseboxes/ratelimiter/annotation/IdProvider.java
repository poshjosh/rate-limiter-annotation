package com.looseboxes.ratelimiter.annotation;

import java.lang.reflect.Method;

/**
 * Provide an id
 * @param <SOURCE> The type of the object for which an id is to be provided
 * @param <ID> The type of the id
 */
public interface IdProvider<SOURCE, ID> {

    static IdProvider<Class<?>, String> ofClass() {
        return new ClassNameProvider();
    }

    static IdProvider<Method, String> ofMethod() {
        return new MethodNameProvider();
    }

    ID getId(SOURCE source);
}
