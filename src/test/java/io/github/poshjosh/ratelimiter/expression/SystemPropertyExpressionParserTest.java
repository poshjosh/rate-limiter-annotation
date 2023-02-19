package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

class SystemPropertyExpressionParserTest extends AbstractMappingStringExpressionParserTest {

    @Override ExpressionParser<Object, String> getExpressionParser() {
        return ExpressionParser.ofSystemProperty();
    }

    @Override String getLHS() {
        return SystemPropertyExpressionParser.LHS;
    }

    private static class ValidParseArgumentProviderForSystemProp extends ValidParseArgumentsProvider {
        public ValidParseArgumentProviderForSystemProp() {
            super(SystemPropertyExpressionParser.LHS, "user.home", System::getProperty);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValidParseArgumentProviderForSystemProp.class)
    void parse_shouldSucceed_givenValidExpression(String expressionStr, Expression<String> expected) {
        super.parse_shouldSucceed_givenValidExpression(expressionStr, expected);
    }
}