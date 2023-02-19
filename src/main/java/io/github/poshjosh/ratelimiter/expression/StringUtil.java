package io.github.poshjosh.ratelimiter.expression;

import io.github.poshjosh.ratelimiter.util.StringUtils;

final class StringUtil {
    private StringUtil() {}
    static String without(String value, String prefix, String suffix) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.startsWith(prefix)) {
            value = value.substring(prefix.length());
        }
        if (value.endsWith(suffix)) {
            value = value.substring(0, value.length() - suffix.length());
        }
        return value;
    }
}
