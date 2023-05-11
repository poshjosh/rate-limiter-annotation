package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SplitterTest {

    private final Splitter splitter = Splitter.EXPRESSION_SPLITTER;

    @ParameterizedTest
    @CsvSource({
            "web.invalid.uri,=,/abc?key1=val1",
            "sys.memory.free,=,1_000",
            "sys.memory.free,!<=,1_000",
            "     \tweb.request.user.role ,  !$,  ROLE_ADMIN  "
    })
    void testValidExpressions(String lhs, String operator, String rhs) {
        String expression = lhs + operator + rhs;
        String [] parts = splitter.split(expression);
        assertEquals(parts.length, 3);
        assertEquals(lhs, parts[0]);
        assertEquals(operator, parts[1]);
        assertEquals(rhs, parts[2]);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "lhs>>>rhs",
            "lhs|",
            "1_000"
    })
    void testInValidExpressions(String expression) {
        assertThrows(RuntimeException.class, () -> splitter.split(expression));
    }
}