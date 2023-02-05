package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SystemMemoryExpressionParserTest {

    @Test
    void shouldSupport() {
        assertTrue(ExpressionParser.ofSystemMemory().isSupported(SystemMemoryExpressionParser.MEMORY_AVAILABLE+"="));
    }

    @Test
    void shouldNotSupport() {
        assertFalse(ExpressionParser.ofSystemMemory().isSupported("sys.time="));
    }

    @ParameterizedTest
    @CsvSource({
            "sys.memory.free=1,0",
            "sys.memory.free>1b,0",
            "sys.memory.free>=1Kb,1",
            "sys.memory.free<1MB,2",
            "sys.memory.free<=1GB,3",
            "sys.memory.free!=1TB,4",
            "sys.memory.free=1PB,5",
            "sys.memory.free=1EB,6",
            "sys.memory.free=1ZB,7",
            "sys.memory.free=1YB,8"
    })
    void shouldSucceed_givenValidExpression(String value, String power) {
        final long expected = (long)Math.pow(1000, Long.parseLong(power));
        Expression<Long> result = ExpressionParser.ofSystemMemory().parse(this, Expression.of(value));
        assertEquals(expected, result.requireRight());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sys.,time,<1,1",
            "sys,ss",
            ""
    })
    void shouldFail_givenInvalidExpression(String value) {
        assertThrows(RuntimeException.class, () ->
                ExpressionParser.ofSystemMemory().parse(this, Expression.of(value)));
    }
}