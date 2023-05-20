package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JvmMemoryExpressionParserTest {

    @Test
    void shouldSupport() {
        assertTrue(ExpressionParser.ofSystemMemory().isSupported(
                JvmMemoryExpressionParser.MEMORY_AVAILABLE+"="));
    }

    @Test
    void shouldNotSupport() {
        assertFalse(ExpressionParser.ofSystemMemory().isSupported("sys.time="));
    }

    @ParameterizedTest
    @CsvSource({
            "jvm.memory.free=1,0",
            "jvm.memory.free>1b,0",
            "jvm.memory.free>=1Kb,1",
            "jvm.memory.free<1MB,2",
            "jvm.memory.free<=1GB,3",
            "jvm.memory.free!=1TB,4",
            "jvm.memory.free=1PB,5",
            "jvm.memory.free=1EB,6",
            "jvm.memory.free=1ZB,7",
            "jvm.memory.free=1YB,8"
    })
    void shouldSucceed_givenValidExpression(String value, String power) {
        final long expected = (long)Math.pow(1000, Long.parseLong(power));
        Expression<Long> result = ExpressionParser.ofSystemMemory().parse(this, Expression.of(value));
        assertEquals(expected, result.requireRight());
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidExpressionArgumentsProvider.class)
    void shouldFail_givenInvalidExpression(String value) {
        assertThrows(RuntimeException.class, () ->
                ExpressionParser.ofSystemMemory().parse(this, Expression.of(value)));
    }
}