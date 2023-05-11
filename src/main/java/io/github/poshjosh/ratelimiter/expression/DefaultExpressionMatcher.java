package io.github.poshjosh.ratelimiter.expression;

import io.github.poshjosh.ratelimiter.util.Matcher;
import io.github.poshjosh.ratelimiter.util.StringUtils;
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
            LOG.trace("Result: {}, expression: typed {}, text {}",
                    success, typedExpression, expression);
        }
        return success ? resolveId(expression, typedExpression) : Matcher.NO_MATCH;
    }

    /**
     * In certain cases, the ID of the typed expression, rather than the ID of the raw
     * (i.e string) expression, could be used as an identifier. For example:
     * <p>
     * Expression: web.session.id!= results to typed expression: <SESSION_ID_VALUE>!=
     * </p>
     * Here, the typed expression is a more suitable identifier because it contains the actual
     * value of the session ID.
     * <br/><br/>
     * However, there are cases when the typed expression changes with time. For example:
     * <p>
     * Expression: sys.time.elapsed>100, results to typed expression: <TIME_ELAPSED_MILLIS>!=100
     * </p>
     * Here, the time elapsed in millis changes with time, hence it is not a suitable identifier.
     *
     * @param expression The expression
     * @param typedExpression The typed expression resolved from the expression
     * @return A suitable ID
     */
    private String resolveId(Expression<String> expression, Expression<T> typedExpression) {
        return !StringUtils.hasText(expression.getRightOrDefault("")) ?
                typedExpression.getId() : expression.getId();
    }

    @Override
    public DefaultExpressionMatcher<R, T> matcher(Expression<String> expression) {
        return new DefaultExpressionMatcher<>(expressionParser, expressionResolver, expression);
    }

    @Override
    public boolean isSupported(Expression<String> expression) {
        return expressionResolver.isSupported(expression.getOperator())
                && expressionParser.isSupported(expression);
    }
}
