package io.github.poshjosh.ratelimiter.matcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SystemTimeElapsedExpressionParserTest {

    @Test
    void a() {
        System.out.println(Duration.ofHours(1).toMillis());
        System.out.println(Duration.ofDays(1).toMillis());
    }

    @Test
    void shouldSupport() {
        assertTrue(ExpressionParser.ofSystemTimeElapsed().isSupported(
                SystemTimeElapsedExpressionParser.TIME_ELAPSED+"="));
    }

    @Test
    void shouldNotSupport() {
        assertFalse(ExpressionParser.ofSystemTimeElapsed().isSupported("sys.memory="));
    }

    @ParameterizedTest
    @CsvSource({
            SystemTimeElapsedExpressionParser.TIME_ELAPSED+"<PT1S",
            SystemTimeElapsedExpressionParser.TIME_ELAPSED+"<PT1H",
            SystemTimeElapsedExpressionParser.TIME_ELAPSED+"<PT24H",
    })
    void shouldSucceed_givenValidExpression(String value) {
        ExpressionParser.ofSystemTimeElapsed().parse(this, Expression.of(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sys.,time,<1,1"
    })
    void shouldFail_givenInvalidExpression(String value) {
      assertThrows(RuntimeException.class, () -> 
              ExpressionParser.ofSystemTimeElapsed().parse(this, Expression.of(value)));
    }
}