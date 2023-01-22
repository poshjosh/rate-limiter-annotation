package io.github.poshjosh.ratelimiter.annotation.exceptions;

public class DuplicateNameException extends AnnotationProcessingException{

    public DuplicateNameException() {
        super("The name of a rate related annotation may only be used once");
    }

    public DuplicateNameException(String name, Object existingUse, Object attemptedUse) {
        super("Name " + name + ", already used at " + existingUse + " may not be re-used at: " + attemptedUse);
    }
}
