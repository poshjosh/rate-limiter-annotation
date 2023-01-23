package io.github.poshjosh.ratelimiter.matcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class JvmThreadExpressionParserTest {

    @Test
    void shouldSupport() {
        assertTrue(ExpressionParser.ofJvmThread().isSupported(
                JvmThreadExpressionParser.COUNT+"="));
    }

    @Test
    void shouldNotSupport() {
        assertFalse(ExpressionParser.ofJvmThread().isSupported("invalid="));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            JvmThreadExpressionParser.COUNT+">0",
    })
    void shouldSucceed_givenValidExpression(String value) {
        ExpressionParser.ofJvmThread().parse("", Expression.of(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid=<0",
            "invalid,<1,1",
            "sys.memory=<1"
    })
    void shouldFail_givenInvalidExpression(String value) {
      assertThrows(RuntimeException.class, () -> 
              ExpressionParser.ofJvmThread().parse("", Expression.of(value)));
    }
}