package io.github.poshjosh.ratelimiter.matcher;

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
    public String matchOrNull(R request) {
        Expression<T> typedExpression = expressionParser.parse(request, expression);
        boolean success = expressionResolver.resolve(typedExpression);
        LOG.debug("Result: {}, expr typed: {}, text: {}", success, typedExpression, expression);
        return success ? expression.getId() : null;
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
