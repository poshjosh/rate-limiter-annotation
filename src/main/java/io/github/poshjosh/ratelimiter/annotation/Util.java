package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotation.exceptions.AnnotationProcessingException;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

final class Util {
    private Util() { }
    static <A extends Annotation> Class<? extends Annotation> getMetaAnnotationTypeOrNull(
            AnnotatedElement source, Class<A> type) {
        Annotation[] annotations = source.getAnnotations();
        A[] rates = null;
        Class<? extends Annotation> metaAnnotationType = null;
        for(Annotation annotation : annotations) {
            // TODO: memory - annotationType().getAnnotationsByType consumes much memory.
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
        return metaAnnotationType;
    }
}
