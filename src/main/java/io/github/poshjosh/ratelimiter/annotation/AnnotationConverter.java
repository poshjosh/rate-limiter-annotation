package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.util.Rates;

public interface AnnotationConverter<I, O> {

    static AnnotationConverter<Rate, Rates> ofRate() {
        return new RateAnnotationConverter();
    }

    Class<I> getAnnotationType();

    O convert(RateGroup rateGroup, Element element, I[] rates);
}
