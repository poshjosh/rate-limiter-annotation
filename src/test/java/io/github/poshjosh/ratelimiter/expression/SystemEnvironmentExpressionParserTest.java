package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class SystemEnvironmentExpressionParserTest extends AbstractMappingStringExpressionParserTest {

    @Override ExpressionParser<Object, String> getExpressionParser() {
        return ExpressionParser.ofSystemEnvironment();
    }

    @Override String getLHS() {
        return SystemEnvironmentExpressionParser.LHS;
    }


    private static class ValidParseArgumentProviderForSystemEnv extends ValidParseArgumentsProvider {
        public ValidParseArgumentProviderForSystemEnv() {
            super(SystemEnvironmentExpressionParser.LHS, "LANG", System::getenv);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ValidParseArgumentProviderForSystemEnv.class)
    void parse_shouldSucceed_givenValidExpression(String expressionStr, Expression<String> expected) {
        super.parse_shouldSucceed_givenValidExpression(expressionStr, expected);
    }
}