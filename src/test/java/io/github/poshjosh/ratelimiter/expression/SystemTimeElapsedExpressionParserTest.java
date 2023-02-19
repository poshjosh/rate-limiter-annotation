package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;

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
    @ArgumentsSource(InvalidExpressionArgumentsProvider.class)
    void shouldFail_givenInvalidExpression(String value) {
      assertThrows(RuntimeException.class, () -> 
              ExpressionParser.ofSystemTimeElapsed().parse(this, Expression.of(value)));
    }
}