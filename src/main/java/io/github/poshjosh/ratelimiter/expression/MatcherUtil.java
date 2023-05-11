package io.github.poshjosh.ratelimiter.expression;

import io.github.poshjosh.ratelimiter.util.StringUtils;

import java.util.*;

final class MatcherUtil {

    private MatcherUtil() { }

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
                result.add(text.substring(pivot + 1, i));
                result.add(Character.toString(ch));
                pivot = i;
            }
        }
        if (pivot != -1) {
            result.add(text.substring(pivot + 1));
        }
        return result.isEmpty() ? new String[]{text} : result.toArray(new String[0]);
    }
}
