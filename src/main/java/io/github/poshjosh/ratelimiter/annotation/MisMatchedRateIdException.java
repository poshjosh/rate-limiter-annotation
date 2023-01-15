package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;

public final class MisMatchedRateIdException extends AnnotationProcessingException{

    public MisMatchedRateIdException() {
        super("Multiple " + Rate.class.getSimpleName() +
                " annotations on a single node must resolve to only one unique name");
    }

    public MisMatchedRateIdException(String message) {
        super(message);
    }
}
