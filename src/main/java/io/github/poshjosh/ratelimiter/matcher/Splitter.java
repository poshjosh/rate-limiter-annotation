package io.github.poshjosh.ratelimiter.matcher;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Splitter {

    // Max length of operator is 3 for now e.g !<= or !>=
    private static final Pattern operatorPattern =
            Pattern.compile("[" + Pattern.quote("=><^$%!") + "]{1,3}");

    static Splitter ofExpression() {
        return new Splitter(operatorPattern, false);
    }

    static Splitter of(Pattern pattern) {
        return new Splitter(pattern, false);
    }

    private final boolean lenient;
    private final Pattern pattern;

    private Splitter(Pattern pattern, boolean lenient) {
        this.pattern = Objects.requireNonNull(pattern);
        this.lenient = lenient;
    }

    public Splitter lenient() {
        return lenient ? this : new Splitter(pattern, true);
    }

    public String[] split(String value) {
        Matcher m = pattern.matcher(value);
        final String lhs;
        final String operator;
        final String rhs;
        if (m.find()) {
            lhs = value.substring(0, m.start()).trim();
            operator = value.substring(m.start(), m.end()).trim();
            rhs = value.substring(m.end()).trim();
        } else {
            throw Checks.notSupported(this, value);
        }
        String [] result = new String[]{lhs, operator, rhs};
        if (lenient) {
            return result;
        }
        for (String e : result) {
            if (e.isEmpty()) {
                throw Checks.notSupported(this, value);
            }
        }
        return result;
    }
}
