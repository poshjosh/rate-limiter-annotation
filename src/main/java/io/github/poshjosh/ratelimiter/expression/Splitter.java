package io.github.poshjosh.ratelimiter.expression;

import io.github.poshjosh.ratelimiter.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Splitter {

    // Max length of operator is 3 for now e.g !<= or !>=
    private static final Pattern operatorPattern =
            Pattern.compile("[" + Pattern.quote("=><^$%!") + "]{1,3}");

    private Splitter() { }

    public static String[] splitExpression(String value) {
        Matcher m = operatorPattern.matcher(value);
        final String lhs;
        final String operator;
        final String rhs;
        if (m.find()) {
            lhs = value.substring(0, m.start()).trim();
            operator = value.substring(m.start(), m.end()).trim();
            rhs = value.substring(m.end()).trim();
        } else {
            throw Checks.notSupported(Splitter.class, value);
        }
        if (operator.isEmpty()) {
            throw Checks.notSupported(Splitter.class, value);
        }
        Operator.of(operator); // Check that this can be converted to a valid operator
        return new String[]{lhs, operator, rhs};
    }


    /**
     * Input: <code>"sys.memory.free=2G|sys.time.elapsed=PT9M"</code>
     * Output: <code>["sys.memory.free=2G", "|", "sys.time.elapsed=PT9M"]</code>
     * @param text The text to split
     * @return The result of splitting the value into expressions and conjunctors.
     */
    static String [] splitIntoExpressionsAndConjunctors(String text) {
        if (!StringUtils.hasText(text)) {
            return new String[]{text};
        }
        boolean opened = false;
        int pivot = -1;
        List<String> result = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ('[' == ch || '{' == ch) {
                opened = true;
            } else if (']' == ch || '}' == ch) { // TODO - Support nesting
                opened = false;
            }
            if (!opened && ('|' == ch || '&' == ch)) { // TODO - support || and &&
                result.add(text.substring(pivot + 1, i).trim());
                result.add(Character.toString(ch));
                pivot = i;
            }
        }
        if (pivot != -1) {
            result.add(text.substring(pivot + 1).trim());
        }
        return result.isEmpty() ? new String[]{text} : result.toArray(new String[0]);
    }
}
