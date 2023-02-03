package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SystemTimeExpressionParserTest {

    @Test
    void shouldSupport() {
        assertTrue(ExpressionParser.ofSystemTime().isSupported(
                SystemTimeExpressionParser.TIME+"="));
    }

    @Test
    void shouldNotSupport() {
        assertFalse(ExpressionParser.ofSystemTime().isSupported("sys.memory="));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            SystemTimeExpressionParser.TIME+"=2023-01-18T20:14:32.846",
            SystemTimeExpressionParser.TIME+"<2023-01-18T20:14:32",
            SystemTimeExpressionParser.TIME+"<=2023-01-18T20:14",
    })
    void shouldSucceed_givenValidExpression(String value) {
        ExpressionParser.ofSystemTime().parse(this, Expression.of(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sys.time=<2023-01-18T20:14:32.846",
            "sys.,memory,<1,1",
            "sys.memory=<1"
    })
    void shouldFail_givenInvalidExpression(String value) {
      assertThrows(RuntimeException.class, () -> 
              ExpressionParser.ofSystemTime().parse(this, Expression.of(value)));
    }
}