package io.github.poshjosh.ratelimiter.matcher;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionResolverTest {

    @ParameterizedTest
    @CsvSource({
            "1,=,1,true",
            "1,>,1,false",
            "1,>=,1,true",
            "1,<,1,false",
            "1,<=,1,true"
    })
    void testValidLongExpression(String lhs, String operator, String rhs, String expectedResult) {
        Long l = Long.parseLong(lhs);
        Long r = Long.parseLong(rhs);
        testValidNumericExpression(ExpressionResolver.ofLong(), l, operator, r, expectedResult);
    }

    @ParameterizedTest
    @CsvSource({
            "1.0,=,1.0,true",
            "1,>,1,false",
            "1,>=,1,true",
            "1,<,1,false",
            "1,<=,1,true"
    })
    void testValidDecimalExpression(String lhs, String operator, String rhs, String expectedResult) {
        Double l = Double.parseDouble(lhs);
        Double r = Double.parseDouble(rhs);
        testValidNumericExpression(ExpressionResolver.ofDecimal(), l, operator, r, expectedResult);
    }

    <T> void testValidNumericExpression(ExpressionResolver<T> resolver,
            T l, String operator, T r, String expectedResult) {
        Expression<T> expression = Expression.of(l, operator, r);
        boolean expected = Boolean.parseBoolean(expectedResult);
        boolean result = resolver.resolve(expression);
        assertEquals(expected, result);
        result = resolver.resolve(expression.flipOperator());
        assertNotEquals(expected, result);
    }
}