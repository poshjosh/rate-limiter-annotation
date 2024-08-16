package io.github.poshjosh.ratelimiter.annotation.exceptions;

public class DuplicateNameException extends AnnotationProcessingException{

    public DuplicateNameException() {
        this("The id of a rate definition may only be used once");
    }

    public DuplicateNameException(String message) {
        super(message);
    }

    public DuplicateNameException(String name, Object existingUse, Object attemptedUse) {
        super("Name " + name + ", already used at " + existingUse + " may not be re-used at: " + attemptedUse);
    }
}
