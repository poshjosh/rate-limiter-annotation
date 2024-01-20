package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;

import java.lang.reflect.GenericDeclaration;

public interface AnnotationConverter {

    static AnnotationConverter ofDefaults() {
        return new RateAnnotationConverter();
    }

    default Class<Rate> getAnnotationType() {
        return Rate.class;
    }

    Rates convert(GenericDeclaration source);
}
