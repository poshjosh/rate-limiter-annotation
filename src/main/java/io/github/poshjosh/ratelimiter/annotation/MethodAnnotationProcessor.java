package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Rates;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

class MethodAnnotationProcessor extends AbstractAnnotationProcessor<Method, Rate, Rates>{

    MethodAnnotationProcessor(AnnotationConverter<Rate, Rates> annotationConverter) {
        super(annotationConverter);
    }

    @Override protected Element toElement(Method element) {
        return Element.of(element);
    }

    @Override protected Node<RateConfig> findExistingParent(Node<RateConfig> root, Method element) {
        return getParentNodeOrDefault(element, root);
    }

    private Node<RateConfig> getParentNodeOrDefault(Method method, Node<RateConfig> resultIfNone) {
        return getDeclaringClassNode(resultIfNone, method).orElse(resultIfNone);
    }

    private Optional<Node<RateConfig>> getDeclaringClassNode(Node<RateConfig> root, Method method) {
        final String declaringClassId = ElementId.of(method.getDeclaringClass());
        Predicate<Node<RateConfig>> testForDeclaringClass = node -> {
            RateConfig rateConfig = node.getValueOrDefault(null);
            if (rateConfig == null) {
                return false;
            }
            return declaringClassId.equals(((Element)rateConfig.getSource()).getId());
        };
        return root.findFirstChild(testForDeclaringClass);
    }
}
