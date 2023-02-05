package io.github.poshjosh.ratelimiter.expression;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Splitter {

    private static final class ExpressionSplitter extends Splitter{
        // Max length of operator is 3 for now e.g !<= or !>=
        private static final Pattern operatorPattern =
                Pattern.compile("[" + Pattern.quote("=><^$%!") + "]{1,3}");
        private ExpressionSplitter() {
            super(operatorPattern);
        }
        @Override
        public String[] split(String value) {
            String [] parts = super.split(value);
            Operator.of(parts[1]); // Check that this can be converted to a valid operator
            return parts;
        }
    }

    static Splitter ofExpression() { return new ExpressionSplitter(); }

    static Splitter of(Pattern pattern) {
        return new Splitter(pattern);
    }

    private final Pattern pattern;

    private Splitter(Pattern pattern) {
        this.pattern = Objects.requireNonNull(pattern);
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
        if (operator.isEmpty()) {
            throw Checks.notSupported(this, value);
        }
        return new String[]{lhs, operator, rhs};
    }
}
