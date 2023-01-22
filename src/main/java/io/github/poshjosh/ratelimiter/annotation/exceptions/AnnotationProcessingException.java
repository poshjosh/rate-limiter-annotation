package io.github.poshjosh.ratelimiter.annotation.exceptions;

public class AnnotationProcessingException extends RuntimeException{
    public AnnotationProcessingException() {}
    public AnnotationProcessingException(String message) {
        super(message);
    }
}
