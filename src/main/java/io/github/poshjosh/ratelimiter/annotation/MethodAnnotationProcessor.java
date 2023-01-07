package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.node.Node;

import java.lang.reflect.Method;
import java.util.function.Predicate;

class MethodAnnotationProcessor extends AbstractAnnotationProcessor<Method> {

    MethodAnnotationProcessor() { }

    @Override protected Element toElement(Method element) {
        return Element.of(element);
    }

    @Override
    protected Node<RateConfig> getOrCreateParent(
            Node<RateConfig> root, Method method,
            RateGroup rateGroup, Rate[] rates) {

        Predicate<Node<RateConfig>> testForDeclaringClass = node -> {
            RateConfig rateConfig = node == null ? null : node.getValueOrDefault(null);
            return rateConfig != null && method.getDeclaringClass().equals(rateConfig.getSource());
        };

        Node<RateConfig> nodeForDeclaringClass = root == null ? null : root.findFirstChild(testForDeclaringClass).orElse(null);

        Node<RateConfig> nodeForRateLimitGroup = findOrCreateNodeForRateLimitGroupOrNull(
                root, nodeForDeclaringClass, method, rateGroup, rates);

        return nodeForRateLimitGroup == null ? nodeForDeclaringClass : nodeForRateLimitGroup;
    }
}
