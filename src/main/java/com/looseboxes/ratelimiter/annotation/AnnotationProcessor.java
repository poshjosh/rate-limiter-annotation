package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.util.Operator;

import java.lang.reflect.GenericDeclaration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public interface AnnotationProcessor<S extends GenericDeclaration> {

    Operator DEFAULT_OPERATOR = Operator.OR;

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
        return new ClassAnnotationProcessor();
    }

    default void processAll(Node<RateConfig> root, S... elements) {
        processAll(root, Arrays.asList(elements));
    }

    default void processAll(Node<RateConfig> root, List<S> elements) {
        processAll(root, (element, node) -> {}, elements);
    }

    default void processAll(Node<RateConfig> root, NodeConsumer consumer, S... elements) {
        processAll(root, consumer, Arrays.asList(elements));
    }

    default void processAll(Node<RateConfig> root, NodeConsumer consumer, List<S> elements) {
        elements.forEach(clazz -> process(root, consumer, clazz));
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
