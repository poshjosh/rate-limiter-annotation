package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SystemTimeElapsedExpressionParserTest {

    private final ExpressionParser<Object, Long> expressionParser = ExpressionParser.ofSystemTimeElapsed();

    @Test
    void a() {
        System.out.println(Duration.ofHours(1).toMillis());
        System.out.println(Duration.ofDays(1).toMillis());
    }

    @Test
    void shouldSupport() {
        assertTrue(expressionParser.isSupported(SystemTimeElapsedExpressionParser.TIME_ELAPSED+"="));
    }

    @Test
    void shouldNotSupport() {
        assertFalse(expressionParser.isSupported("sys.memory="));
    }

    @ParameterizedTest
    @CsvSource({
            SystemTimeElapsedExpressionParser.TIME_ELAPSED+"<PT1S",
            SystemTimeElapsedExpressionParser.TIME_ELAPSED+"<PT1H",
            SystemTimeElapsedExpressionParser.TIME_ELAPSED+"<PT24H",
    })
    void shouldSucceed_givenValidExpression(String value) {
        expressionParser.parse(System.currentTimeMillis(), Expression.of(value));
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidExpressionArgumentsProvider.class)
    void shouldFail_givenInvalidExpression(String value) {
      assertThrows(RuntimeException.class, () -> 
              expressionParser.parse(System.currentTimeMillis(), Expression.of(value)));
    }
}