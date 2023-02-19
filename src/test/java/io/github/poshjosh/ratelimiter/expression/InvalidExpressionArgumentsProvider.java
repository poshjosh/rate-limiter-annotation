package io.github.poshjosh.ratelimiter.expression;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class InvalidExpressionArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext){
        return Stream.of(
                "a.,b,<1,1",
                "sys,ss",
                ",",
                "",
                "invalid=<",
                "sys.memory=<1"

        ).map(Arguments::of);
    }
}
