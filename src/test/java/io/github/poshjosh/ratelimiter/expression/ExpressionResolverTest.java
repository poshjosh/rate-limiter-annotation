package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionResolverTest {

    @ParameterizedTest
    @CsvSource({
            JvmThreadExpressionParser.COUNT + ">=0,true",
            JvmThreadExpressionParser.COUNT_DAEMON + ">=0,true",
            JvmThreadExpressionParser.COUNT_DEADLOCKED + ">=0,true",
            JvmThreadExpressionParser.COUNT_DEADLOCKED_MONITOR + ">=0,true",
            JvmThreadExpressionParser.COUNT_PEAK + ">=0,true",
            JvmThreadExpressionParser.COUNT_STARTED + ">=0,true",
            JvmThreadExpressionParser.CURRENT_COUNT_BLOCKED + ">=0,true",
            JvmThreadExpressionParser.CURRENT_COUNT_WAITED + ">=0,true",
            JvmThreadExpressionParser.CURRENT_STATE + "=RUNNABLE,true",
            JvmThreadExpressionParser.CURRENT_STATE + "=BLOCKED,false",
            JvmThreadExpressionParser.CURRENT_SUSPENDED + "=false,true",
            JvmThreadExpressionParser.CURRENT_TIME_BLOCKED + "<=PT0S,true",
            JvmThreadExpressionParser.CURRENT_TIME_CPU + ">=PT0S,true",
            JvmThreadExpressionParser.CURRENT_TIME_USER + ">=PT0S,true",
            JvmThreadExpressionParser.CURRENT_TIME_WAITED + "<=PT0S,true",
    })
    void testJvmThreadExpression(String expressionString, String expectedResult) {
        Expression<Object> expression = ExpressionParser.ofJvmThread()
                .parse(this, Expression.of(expressionString));
        //System.out.println(expression);
        testExpression(ExpressionResolver.ofJvmThread(), expression, expectedResult);
    }

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
        testExpression(ExpressionResolver.ofLong(), l, operator, r, expectedResult);
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
        testExpression(ExpressionResolver.ofDecimal(), l, operator, r, expectedResult);
    }

    void testExpression(ExpressionResolver<?> resolver,
            Object l, String operator, Object r, String expectedResult) {
        Expression<?> expression = Expression.of(l, operator, r);
        testExpression(resolver, expression, expectedResult);
    }

    void testExpression(ExpressionResolver<?> resolver, Expression expression, String expectedResult) {
        boolean expected = Boolean.parseBoolean(expectedResult);
        boolean result = resolver.resolve(expression);
        assertEquals(expected, result);
        result = resolver.resolve(expression.flipOperator());
        assertNotEquals(expected, result);
    }
}