package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.RateLimit;
import com.looseboxes.ratelimiter.annotations.RateLimitGroup;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Operator;
import com.looseboxes.ratelimiter.util.Rates;

import java.lang.reflect.GenericDeclaration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface AnnotationProcessor<S extends GenericDeclaration, T> {

    Operator DEFAULT_OPERATOR = Operator.OR;

    interface NodeConsumer<T>{
        void accept(Object o, Node<NodeValue<T>> node);
        default NodeConsumer<T> andThen(NodeConsumer<T> after) {
            Objects.requireNonNull(after);
            return (l, r) -> {
                accept(l, r);
                after.accept(l, r);
            };
        }
    }

    interface Converter<T>{
        T convert(RateLimitGroup rateLimitGroup, RateLimit[] rateLimits);
        boolean isOperatorEqual(T type, Operator operator);
    }

    static AnnotationProcessor<Class<?>, Rates> ofRates() {
        return of(new AnnotationToRatesConverter());
    }

    static <T> AnnotationProcessor<Class<?>, T> of(Converter<T> converter) {
        return new ClassAnnotationProcessor<>(converter);
    }

    default void processAll(Node<NodeValue<T>> root, S... elements) {
        processAll(root, elements == null ? Collections.emptyList() : Arrays.asList(elements));
    }

    default void processAll(Node<NodeValue<T>> root, List<S> elements) {
        processAll(root, (element, node) -> {}, elements);
    }

    default void processAll(Node<NodeValue<T>> root, NodeConsumer<T> consumer, S... elements) {
        processAll(root, consumer, elements == null ? Collections.emptyList() : Arrays.asList(elements));
    }

    default void processAll(Node<NodeValue<T>> root, NodeConsumer<T> consumer, List<S> elements) {
        elements.forEach(clazz -> process(root, consumer, clazz));
    }

    default Node<NodeValue<T>> process(S element) {
        return process(Node.of("root"), (obj, node) -> { }, element);
    }

    /**
     * @param root the root node
     * @param consumer a consumer that will be applied to each node processed
     * @param element the element for which rate limit annotations will be processed
     * @return The root node
     */
    Node<NodeValue<T>> process(Node<NodeValue<T>> root, NodeConsumer<T> consumer, S element);
}
