package io.github.poshjosh.ratelimiter.expression;

import io.github.poshjosh.ratelimiter.util.Matcher;

final class ExpressionMatcherComposite<R> implements ExpressionMatcher<R, Object>{

    private ExpressionMatcher<R, ?> [] expressionMatchers;

    ExpressionMatcherComposite(ExpressionMatcher<R, ?>... matchers) {
        this.expressionMatchers = new ExpressionMatcher[matchers.length];
        System.arraycopy(matchers, 0, this.expressionMatchers, 0, matchers.length);
    }

    @Override
    public String match(R request) {
        StringBuilder result = new StringBuilder(32 * expressionMatchers.length);
        for (ExpressionMatcher<R, ?> expressionMatcher : expressionMatchers) {
            final String match = expressionMatcher.match(request);
            if (!Matcher.isMatch(match)) {
                return match;
            }
            if (result.length() > 0) {
                result.append('_');
            }
            result.append(match);
        }
        return result.toString();
    }

    @Override
    public ExpressionMatcher<R, Object> with(Expression<String> expression) {
        for (ExpressionMatcher<R, ?> expressionMatcher : expressionMatchers) {
            if (expressionMatcher.isSupported(expression)) {
                return (ExpressionMatcher<R, Object>)expressionMatcher.with(expression);
            }
        }
        throw Checks.notSupported(this, expression);
    }

    @Override
    public boolean isSupported(Expression<String> expression) {
        for (ExpressionMatcher<R, ?> expressionMatcher : expressionMatchers) {
            if (expressionMatcher.isSupported(expression)) {
                return true;
            }
        }
        return false;
    }
}
