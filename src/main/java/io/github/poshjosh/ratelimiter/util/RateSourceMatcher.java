package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.annotations.RateGroup;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.Objects;

final class RateSourceMatcher<INPUT> implements Matcher<INPUT> {
    private final String id;
    private final Object source;

    RateSourceMatcher(String id, Object source) {
        this.id = Objects.requireNonNull(id);
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public String match(INPUT input) {
        return doMatch(input);
    }

    private String doMatch(Object input) {
        return doMatch(input, false);
    }

    private String doMatch(Object input, boolean annotationClassDetermined) {

        if (input == null) {
            return Matchers.NO_MATCH;
        }

        if (input instanceof Class) {
            if (Objects.equals(source, input)) {
                return id;
            }
            if (!annotationClassDetermined) {
                final Class<?> clazz = (Class<?>)input;
                Class<?> annotationClass = getRateGroupClassOrDefaultForClass(clazz, null);
                if (annotationClass != null) {
                    return doMatch(annotationClass, true);
                }
            }
        }

        if (input instanceof Method) {
            if (Objects.equals(source, input)) {
                return id;
            }
            final Method method = (Method)input;
            Class<?> annotationClass = getRateGroupClassOrDefaultFor(method, null);
            if (annotationClass != null) {
                return doMatch(annotationClass);
            }
            return doMatch(method.getDeclaringClass());
        }

        if (input instanceof String) {
            if (Objects.equals(id, input)) {
                return id;
            }
            final String sval = (String)input;
            if (source instanceof Class && sval.startsWith(id)) {
                return id;
            }
//
//            GenericDeclaration source = RateId.parse(sval, null);
//            if (source != null) {
//                return doMatch(source);
//            }
        }

        return Matchers.NO_MATCH;
    }

    private Class<?> getRateGroupClassOrDefaultForClass(Class<?> clazz, Class<?> resultIfNone) {
        if (clazz.isAnnotation()) {
            return clazz.getAnnotation(RateGroup.class) != null ? clazz : resultIfNone;
        }
        return getRateGroupClassOrDefaultFor(clazz, resultIfNone);
    }

    private Class<?> getRateGroupClassOrDefaultFor(
            GenericDeclaration source, Class<?> resultIfNone) {
        Annotation[] annotations = source.getAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            boolean rateGroup = annotationType.getAnnotation(RateGroup.class) != null;
            if (rateGroup) {
                // RateGroup is not a repeatable annotation
                // so only one annotation is expected.
                return annotationType;
            }
        }
        return resultIfNone;
    }

    @Override
    public String toString() {
        return "RateSourceMatcher{" + "id='" + id + '\'' + '}';
    }
}
