package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MatcherUtilTest {

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
        String [] actual = MatcherUtil.splitIntoExpressionsAndConjunctors(arr[0]);

        assertArrayEquals(expected, actual,
                "\nExpected: " + Arrays.toString(expected) +
                "\n  Actual: " + Arrays.toString(actual) + "\n");
    }
}