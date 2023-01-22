package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotation.exceptions.AnnotationProcessingException;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

final class Util {
    private Util() { }
    static <A extends Annotation> Optional<Class<? extends Annotation>> getMetaAnnotationType(
            AnnotatedElement source, Class<A> type) {
        Annotation[] annotations = source.getAnnotations();
        A[] rates = null;
        Class<? extends Annotation> metaAnnotationType = null;
        for(Annotation annotation : annotations) {
            A[] found = annotation.annotationType().getAnnotationsByType(type);
            if (found.length == 0) {
                continue;
            }
            if (rates == null) {
                rates = found;
                metaAnnotationType = annotation.annotationType();
            } else{
                throw new AnnotationProcessingException(
                        "Only one meta annotation may convey rates (i.e " + type +
                                "). Found more than one at: " + source);
            }
        }
        return Optional.ofNullable(metaAnnotationType);
    }
}
