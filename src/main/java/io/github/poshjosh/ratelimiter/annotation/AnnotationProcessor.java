package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Rates;

import java.lang.reflect.GenericDeclaration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public interface AnnotationProcessor<S extends GenericDeclaration> {

    @FunctionalInterface
    interface NodeConsumer{
        void accept(Object o, Node<RateConfig> node);
        default NodeConsumer andThen(NodeConsumer after) {
            Objects.requireNonNull(after);
            return (l, r) -> {
                accept(l, r);
                after.accept(l, r);
            };
        }
    }

    static AnnotationProcessor<Class<?>> ofDefaults() {
        return new ClassAnnotationProcessor(AnnotationConverter.ofRate());
    }

    static AnnotationProcessor<Class<?>> of(AnnotationConverter<Rate, Rates> annotationConverter) {
        return new ClassAnnotationProcessor(annotationConverter);
    }

    default Node<RateConfig> processAll(Node<RateConfig> root, S... elements) {
        return processAll(root, Arrays.asList(elements));
    }

    default Node<RateConfig> processAll(Node<RateConfig> root, List<S> elements) {
        return processAll(root, (element, node) -> {}, elements);
    }

    default Node<RateConfig> processAll(Node<RateConfig> root, NodeConsumer consumer, S... elements) {
        return processAll(root, consumer, Arrays.asList(elements));
    }

    default Node<RateConfig> processAll(Node<RateConfig> root, NodeConsumer consumer, List<S> elements) {
        for(S element : elements) {
            root = process(root, consumer, element);
        }
        return root;
    }

    default Node<RateConfig> process(S element) {
        return process(Node.of("root"), (obj, node) -> { }, element);
    }

    /**
     * @param root the root node
     * @param consumer a consumer that will be applied to each node processed
     * @param element the element for which rate limit annotations will be processed
     * @return The root node
     */
    Node<RateConfig> process(Node<RateConfig> root, NodeConsumer consumer, S element);
}
