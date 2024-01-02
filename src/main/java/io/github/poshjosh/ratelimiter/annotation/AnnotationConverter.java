package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;

public interface AnnotationConverter<A extends Annotation, R> {

    static AnnotationConverter<Rate, Rates> ofRate() {
        return new RateAnnotationConverter();
    }

    Class<A> getAnnotationType();

    R convert(GenericDeclaration source);
}
