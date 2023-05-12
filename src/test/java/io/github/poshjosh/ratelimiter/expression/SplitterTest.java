package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

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
        String [] parts = Splitter.splitExpression(expression);
        assertEquals(parts.length, 3);
        assertEquals(lhs, parts[0]);
        assertEquals(operator, parts[1]);
        assertEquals(rhs, parts[2]);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "lhs>>>rhs",
            "lhs|",
            "1_000"
    })
    void testInValidExpressions(String expression) {
        assertThrows(RuntimeException.class, () -> Splitter.splitExpression(expression));
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "sys.time.elapsed>PT1S|sys.time.elapsed>PT0S,sys.time.elapsed>PT1S,|,sys.time.elapsed>PT0S",
            "name_0={key=value}|name_1=value_1,name_0={key=value},|,name_1=value_1",
            "name_0!={key=[value_0|value_1]}|name_1!={key=[value_0&value_1]},name_0!={key=[value_0|value_1]},|,name_1!={key=[value_0&value_1]}"
    })
    void splitIntoExpressionsAndConjunctors(String s) {
        String [] arr = s.split(",");
        String [] expected = new String[arr.length - 1];
        System.arraycopy(arr, 1, expected, 0, expected.length);
        String [] actual = Splitter.splitIntoExpressionsAndConjunctors(arr[0]);

        assertArrayEquals(expected, actual,
                "\nExpected: " + Arrays.toString(expected) +
                        "\n  Actual: " + Arrays.toString(actual) + "\n");
    }
}