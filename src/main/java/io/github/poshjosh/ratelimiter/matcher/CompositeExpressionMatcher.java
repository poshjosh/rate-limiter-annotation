package io.github.poshjosh.ratelimiter.matcher;

final class CompositeExpressionMatcher<R> implements ExpressionMatcher<R, Object>{

    private ExpressionMatcher<R, ?> [] expressionMatchers;

    CompositeExpressionMatcher(ExpressionMatcher<R, ?>... matchers) {
        this.expressionMatchers = new ExpressionMatcher[matchers.length];
        System.arraycopy(matchers, 0, this.expressionMatchers, 0, matchers.length);
    }

    @Override
    public String matchOrNull(R request) {
        for (ExpressionMatcher<R, ?> expressionMatcher : expressionMatchers) {
            String result = expressionMatcher.matchOrNull(request);
            if (result != null) {
                return result;
            }
        }
        return null;
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
