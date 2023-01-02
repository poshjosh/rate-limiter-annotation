package com.looseboxes.ratelimiter.annotation;

public class AnnotationProcessingException extends RuntimeException{
    public AnnotationProcessingException(String message) {
        super(message);
    }
}
