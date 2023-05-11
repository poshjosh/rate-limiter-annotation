package io.github.poshjosh.ratelimiter.expression;

import io.github.poshjosh.ratelimiter.util.Matcher;

final class AnyExpressionMatcher<R> implements ExpressionMatcher<R, Object>{

    private ExpressionMatcher<R, ?> [] expressionMatchers;

    AnyExpressionMatcher(ExpressionMatcher<R, ?>... matchers) {
        this.expressionMatchers = new ExpressionMatcher[matchers.length];
        System.arraycopy(matchers, 0, this.expressionMatchers, 0, matchers.length);
    }

    @Override
    public String match(R request) {
        for (ExpressionMatcher<R, ?> expressionMatcher : expressionMatchers) {
            final String match = expressionMatcher.match(request);
            if (Matcher.isMatch(match)) {
                return match;
            }
        }
        return Matcher.NO_MATCH;
    }

    @Override
    public ExpressionMatcher<R, Object> matcher(Expression<String> expression) {
        for (ExpressionMatcher<R, ?> expressionMatcher : expressionMatchers) {
            if (expressionMatcher.isSupported(expression)) {
                return (ExpressionMatcher<R, Object>)expressionMatcher.matcher(expression);
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
