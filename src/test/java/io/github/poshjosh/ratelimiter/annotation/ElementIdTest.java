package io.github.poshjosh.ratelimiter.annotation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementIdTest {

    // Used by some tests below.
    void methodWithArgs(ZonedDateTime arg1, String arg2) { }

    private String getMethodWithArgsId() {
        return ElementId.ofMethod(getClass(),
                "methodWithArgs", ZonedDateTime.class, String.class);
    }

    @Test
    void shouldReturnValidClassId() {
        String found = ElementId.of(getClass());
        assertEquals(getClass().getName(), found);
    }

    @Test
    void shouldReturnValidMethodId() {
        String found = ElementId.ofMethod(getClass(), "shouldReturnValidMethodId");
        assertEquals(getClass().getName() + ".shouldReturnValidMethodId()", found);
    }

    @Test
    void shouldReturnValidMethodIdGivenMethodWithArguments() {
        String found = getMethodWithArgsId();
        assertEquals(getClass().getName() +
                ".methodWithArgs(java.time.ZonedDateTime,java.lang.String)", found);
    }

    @Test
    void shouldParseClassPartFromClassId() {
        String expected = ElementId.of(getClass());
        String found = ElementId.parseClassPart(expected).orElse(null);
        assertEquals(expected, found);
    }

    @Test
    void shouldParseClassPartFromMethodId() {
        String methodId = ElementId.ofMethod(getClass(), "shouldParseClassPartFromMethodId");
        String found = ElementId.parseClassPart(methodId).orElse(null);
        assertEquals(ElementId.of(getClass()), found);
    }

    @Test
    void shouldParseClassPartFromMethodIdGivenMethodWithArguments() {
        String methodId = getMethodWithArgsId();
        String found = ElementId.parseClassPart(methodId).orElse(null);
        assertEquals(ElementId.of(getClass()), found);
    }

    @Test
    void shouldReturnEmptyWhenNoClassPartPresent() {
        Assertions.assertFalse(ElementId.parseClassPart("someMethod()").isPresent());
    }
}
