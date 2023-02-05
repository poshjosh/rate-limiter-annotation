package io.github.poshjosh.ratelimiter.expression;

import io.github.poshjosh.ratelimiter.util.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

final class DefaultExpressionMatcher<R, T> implements ExpressionMatcher<R, T> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultExpressionMatcher.class);

    private final ExpressionParser<R, T> expressionParser;

    private final ExpressionResolver<T> expressionResolver;

    private final Expression<String> expression;

    DefaultExpressionMatcher(
            ExpressionParser<R, T> expressionParser,
            ExpressionResolver<T> expressionResolver,
            Expression<String> expression) {
        if (!expressionResolver.isSupported(expression.getOperator())) {
            throw Checks.notSupported(expressionResolver,
                    "operator: " + expression.getOperator());
        }
        if (!expressionParser.isSupported(expression)) {
            throw Checks.notSupported(expressionParser, expression);
        }
        this.expressionParser = Objects.requireNonNull(expressionParser);
        this.expressionResolver = Objects.requireNonNull(expressionResolver);
        this.expression = Objects.requireNonNull(expression);
    }

    @Override
    public String match(R request) {
        Expression<T> typedExpression = expressionParser.parse(request, expression);
        boolean success = expressionResolver.resolve(typedExpression);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Result: {}, expression typed: {}, text: {}",
                    success, typedExpression, expression);
        }
        // We use the ID of the typed expression rather than the raw (i.e string) expression
        // Expression: web.session.id!= results to typed expression: <SESSION_ID_VALUE>!=
        // The typed expression is a more suitable identifier because it contains the actual
        // value of the session ID
        return success ? typedExpression.getId() : Matcher.NO_MATCH;
    }

    @Override
    public DefaultExpressionMatcher<R, T> with(Expression<String> expression) {
        return new DefaultExpressionMatcher<>(expressionParser, expressionResolver, expression);
    }

    @Override
    public boolean isSupported(Expression<String> expression) {
        return expressionResolver.isSupported(expression.getOperator())
                && expressionParser.isSupported(expression);
    }
}
