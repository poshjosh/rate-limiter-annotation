package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Experimental;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class RateId {
    private static final Logger LOG = LoggerFactory.getLogger(RateId.class);

    private RateId() { }

    // Currently not used, also does not support Methods with arguments
    @Experimental
    static GenericDeclaration parse(String text, GenericDeclaration resultIfNone) {
        final int indexOfOpenBracket = text.indexOf('(');
        if (indexOfOpenBracket == -1) {
            Class<?> clazz = getClassOrNull(text, text);
            return clazz == null ? resultIfNone : clazz;
        }
        final String classAndMethodPart = text.substring(0, indexOfOpenBracket);
        final int endOfClassPart = classAndMethodPart.lastIndexOf('.');
        if (endOfClassPart == -1) {
            return resultIfNone;
        }
        Class<?> clazz = getClassOrNull(classAndMethodPart.substring(0, endOfClassPart), text);
        if (clazz == null) {
            return resultIfNone;
        }
        try {
            return clazz.getDeclaredMethod(classAndMethodPart.substring(endOfClassPart + 1));
        } catch (NoSuchMethodException e) {
            LOG.debug("Failed to parse method part of: " + text, e);
            return resultIfNone; // A text id could look like a class-method signature
        }
    }

    private static Class<?> getClassOrNull(String name, String text) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            LOG.debug("Failed to parse: " + text, e);
            return null;
        }
    }

    public static String of(GenericDeclaration source) {
        if (source instanceof Class) {
            return of((Class<?>) source);
        }
        if (source instanceof Method) {
            return of((Method) source);
        }
        throw new UnsupportedOperationException("Unsupported source: " + source);
    }

    /**
     * Identify a class.
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
        return getName(clazz);
    }

    /**
     * Identify a method.
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
     * <p>For <pre>methodA</pre> will return <pre>com.example.ExampleClass.methodA()</pre></p>
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
        final int indexOfClassName = methodString.indexOf(getName(method.getDeclaringClass()));
        if(indexOfClassName == -1) {
            // Should not happen
            throw new AssertionError(
                    "Method#toString() does not contain the method's declaring class name as expected. Method: " + method);
        }
        final int indexOfClosingBracket = methodString.indexOf(")");
        if(indexOfClosingBracket == -1) {
            // Should not happen
            throw new AssertionError(
                    "Method#toString() returned unexpected text. Method: " + method);
        }
        return methodString.substring(indexOfClassName, indexOfClosingBracket + 1);
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
        return Arrays.stream(new String[]{rateGroup.value(), rateGroup.id()})
                .filter(StringUtils::hasText)
                .findAny().orElse("");
    }

    private static String getSpecifiedId(AnnotatedElement source, Rate [] rates) {
        if (rates == null || rates.length == 0) {
            return "";
        }
        if (rates.length == 1) {
            return rates[0].id();
        }
        return Checks.requireSameId(source, rates);
    }

    private static String getName(Class<?> clazz) {
        return clazz.getName();
    }
}
