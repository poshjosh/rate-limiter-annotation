package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.util.StringUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class ElementId {

    private ElementId() { }

    /**
     * Identify a class
     *
     * The class is identified either by the name attribute of a {@link Rate} annotation
     * on the class or by an id created from the full class name.
     *
     * @param clazz the class whose ID is to be returned
     * @return An identifier the specified class
     */
    public static String of(Class<?> clazz) {
        final String specifiedId = getSpecifiedId(clazz);
        if ( StringUtils.hasText(specifiedId)) {
            return specifiedId;
        }
        return clazz.getName();
    }

    /**
     * Identify a method.
     *
     * The method is identified either by the name attribute of a {@link Rate} annotation
     * on the method or by an id created from the method signature.
     *
     * <p>Here is how an ID is created from the method signature:</p>
     *
     * Given a class with 2 methods
     *
     * <pre>
     *     package com.example;
     *     class ExampleClass{
     *         void methodA() { }
     *         protected String methodB(Long key, String value) { return value; }
     *     }
     * </pre>
     *
     * <p>For <pre>methodA</pre> will return <pre>void com.example.ExampleClass.methodA()</pre></p>
     *
     * <p>
     *     For <pre>methodB</pre> will return <pre>com.example.ExampleClass.methodB(java.lang.Long,java.lang.String)</pre>
     * </p>
     *
     * @param method The method whose ID is to be returned
     * @return An identifier for the specified method
     */
    public static String of(Method method) {
        final String specifiedId = getSpecifiedId(method);
        if (StringUtils.hasText(specifiedId)) {
            return specifiedId;
        }
        final String methodString = method.toString();
        final int indexOfClassName = methodString.indexOf(method.getDeclaringClass().getName());
        if(indexOfClassName == -1) {
            // Should not happen
            throw new AssertionError("Method#toString() does not contain the method's declaring class name as expected. Method: " + method);
        }
        return methodString.substring(indexOfClassName);
    }

    private static String getSpecifiedId(AnnotatedElement source) {
        // @RateGroup is only allowed on annotation types
        // If the source contains a @RateGroup, then it is an annotation type
        //
        final RateGroup rateGroup = source.getAnnotation(RateGroup.class);
        final String nameFromGroup = getSpecifiedId(rateGroup);
        if (StringUtils.hasText(nameFromGroup)) {
            return nameFromGroup;
        }
        final Rate[] rates = source.getAnnotationsByType(Rate.class);
        return getSpecifiedId(source, rates);
    }

    private static String getSpecifiedId(RateGroup rateGroup) {
        if (rateGroup == null) {
            return "";
        }
        return Arrays.stream(new String[]{rateGroup.value(), rateGroup.name()})
                .filter(StringUtils::hasText)
                .findAny().orElse("");
    }

    private static String getSpecifiedId(AnnotatedElement source, Rate [] rates) {
        if (rates == null || rates.length == 0) {
            return "";
        }
        if (rates.length == 1) {
            return rates[0].name();
        }
        return Checks.requireSameId(rates, source);
    }
}
