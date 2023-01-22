package io.github.poshjosh.ratelimiter.annotation.exceptions;

import io.github.poshjosh.ratelimiter.annotations.Rate;

public final class MisMatchedRateNameException extends AnnotationProcessingException{

    public MisMatchedRateNameException() {
        super("Multiple " + Rate.class.getSimpleName() +
                " annotations on a single node must resolve to only one unique name");
    }

    public MisMatchedRateNameException(String message) {
        super(message);
    }
}
