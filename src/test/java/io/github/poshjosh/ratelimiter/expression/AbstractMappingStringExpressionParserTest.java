package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractMappingStringExpressionParserTest {
    abstract ExpressionParser<Object, String> getExpressionParser();
    abstract String getLHS();

    @Test
    public void shouldSupportOperators() {
        //supportedOperators().forEach(System.out::println);
        //System.out.println(supportedOperators().map(Operator::getSymbol).collect(Collectors.joining(",")));
        supportedOperators().forEach(operator -> assertTrue(
                isSupported(operator), "Should be supported: " + operator));
    }

    @Test
    public void shouldNotSupportOperators() {
        //unsupportedOperators().forEach(System.out::println);
        //System.out.println(unsupportedOperators().map(Operator::getSymbol).collect(Collectors.joining(",)));
        unsupportedOperators().forEach(operator -> assertFalse(
                isSupported(operator), "Should not be supported: " + operator));
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidExpressionArgumentsProvider.class)
    void shouldFail_givenInvalidExpression(String value) {
        assertThrows(RuntimeException.class, () ->
                getExpressionParser().parse("", Expression.of(value)));
    }

    void parse_shouldSucceed_givenValidExpression(String expressionStr, Expression<String> expected) {
        Expression<String> expression = Expression.of(expressionStr);
        Expression<String> actual = getExpressionParser().parse(this, expression);
        //System.out.println("Input: " + expressionStr + ", expected: " + expected + ", actual: " + actual);
        assertEquals(expected, actual);
    }

    private boolean isSupported(Operator operator) {
        return getExpressionParser().isSupported(getLHS() + operator.getSymbol());
    }

    private static Stream<Operator> supportedOperators() {
        return Arrays.stream(Operator.values()).filter(operator -> operator.equalsIgnoreNegation(Operator.EQUALS));
    }

    private static Stream<Operator> unsupportedOperators() {
        return Arrays.stream(Operator.values()).filter(operator -> !operator.equalsIgnoreNegation(Operator.EQUALS));
    }

    static class ValidParseArgumentsProvider implements ArgumentsProvider {
        private final String lhs;
        private final String validName;
        private final UnaryOperator<String> valueProvider;
        public ValidParseArgumentsProvider(
                String lhs, String validName, UnaryOperator<String> valueProvider) {
            this.lhs = Objects.requireNonNull(lhs);
            this.validName = Objects.requireNonNull(validName);
            this.valueProvider = Objects.requireNonNull(valueProvider);
        }
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            final String [] names = {validName, "fake-name"};
            //final String [] names = {"fake-name"};
            final List<Operator> mainOptrs = AbstractMappingStringExpressionParserTest
                    .supportedOperators().collect(Collectors.toList());
            final Operator[] subOptrs = Operator.values();
            //final Operator [] subOptrs = {Operator.EQUALS};
            List<Arguments> args = new ArrayList<>();
            for (String name : names) {
                final String value = valueProvider.apply(name);
                final String rhs = value == null ? "" : value;
                for (Operator mainOptr : mainOptrs) {
                    for (Operator operator : subOptrs) {
                        String arg0 = lhs + mainOptr.getSymbol() + "{" + name + operator.getSymbol() + rhs + "}";
                        args.add(Arguments.of(arg0, Expression.of(value, operator, rhs)));
                    }
                }
            }
            return args.stream();
        }
    }
}
