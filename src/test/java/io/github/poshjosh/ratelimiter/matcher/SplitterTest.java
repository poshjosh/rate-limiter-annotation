package io.github.poshjosh.ratelimiter.matcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SplitterTest {

    @ParameterizedTest
    @CsvSource({
            "web.invalid.uri,=,/abc?key1=val1",
            "sys.memory.free,=,1_000",
            "sys.memory.free,!<=,1_000",
            "     \tweb.request.user.role ,  !$,  ROLE_ADMIN  "
    })
    void testValidExpressions(String lhs, String operator, String rhs) {
        String expression = lhs + operator + rhs;
        String [] parts = Splitter.ofExpression().split(expression);
        assertEquals(parts.length, 3);
        assertEquals(lhs, parts[0]);
        assertEquals(operator, parts[1]);
        assertEquals(rhs, parts[2]);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sys.invalid.free>",
            ">1_000"
    })
    void testInValidExpressions(String expression) {
        assertThrows(RuntimeException.class,
                () -> Splitter.ofExpression().split(expression));
    }
}