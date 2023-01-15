package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.node.Node;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

final class Checks {
    private Checks() {}
    static AnnotationProcessingException exception(String msg) {
        return new AnnotationProcessingException(msg);
    }
    static <V> V requireNodeValue(Node<V> node) {
        return node.getValueOptional().orElseThrow(() -> new NodeValueAbsentException(node));
    }
    static String requireOneContent(Object source, String what, String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isEmpty())
                .findAny().orElseThrow(() ->
                        new AnnotationProcessingException(what + " required at: " + source));
    }
    static String requireSameId(Rate[] rates, Object source) {
        Set<String> uniqueNames = Arrays.stream(rates)
                .map(Rate::name).collect(Collectors.toSet());
        if (uniqueNames.size() > 1) {
            throw new MisMatchedRateIdException(
                    "Multiple " + Rate.class.getSimpleName() +
                            " annotations on a single node must resolve to only one unique name, found: " +
                            uniqueNames + " at " + source);

        }
        return uniqueNames.iterator().next();
    }
    static RuntimeException illegal(Enum en) {
        return new IllegalArgumentException("Unexpected " +
                en.getDeclaringClass().getSimpleName() + ": " + en +
                ", supported: " + Arrays.toString(en.getClass().getEnumConstants()));

    }
}
