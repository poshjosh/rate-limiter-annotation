package io.github.poshjosh.ratelimiter.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateIdTest {

    @Test
    void shouldParseClassText() {
        Class<?> expected = getClass();
        GenericDeclaration found = RateId.parse(RateId.of(expected), null);
        assertEquals(expected, found);
    }

    @Test
    void shouldParseMethodText() {
        Method expected = getMethod("shouldParseMethodText");
        GenericDeclaration found = RateId.parse(RateId.of(expected), null);
        assertEquals(expected, found);
    }

    private void methodWithArguments(ZonedDateTime arg1, Boolean arg2) { }

    @Test
    void shouldParseMethodWithArgumentsText() {
        // TODO
    }

    private Method getMethod(String name, Class<?>... parameterTypes) {
        try {
            return getClass().getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
