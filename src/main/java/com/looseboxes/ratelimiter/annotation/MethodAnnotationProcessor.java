package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.RateLimit;
import com.looseboxes.ratelimiter.annotations.RateLimitGroup;
import com.looseboxes.ratelimiter.node.Node;

import java.lang.reflect.Method;
import java.util.function.Predicate;

final class MethodAnnotationProcessor<T> extends AbstractAnnotationProcessor<Method, T> {

    MethodAnnotationProcessor(IdProvider<Method, String> idProvider, Converter<T> converter) {
        super(idProvider, converter);
    }

    @Override
    protected Node<NodeValue<T>> getOrCreateParent(
            Node<NodeValue<T>> root, Method method,
            RateLimitGroup rateLimitGroup, RateLimit[] rateLimits) {

        Predicate<Node<NodeValue<T>>> testForDeclaringClass = node -> {
            NodeValue<T> nodeValue = node == null ? null : node.getValueOrDefault(null);
            return nodeValue != null && method.getDeclaringClass().equals(nodeValue.getSource());
        };

        Node<NodeValue<T>> nodeForDeclaringClass = root == null ? null : root.findFirstChild(testForDeclaringClass).orElse(null);

        Node<NodeValue<T>> nodeForRateLimitGroup = findOrCreateNodeForRateLimitGroupOrNull(
                root, nodeForDeclaringClass, method, rateLimitGroup, rateLimits);

        return nodeForRateLimitGroup == null ? nodeForDeclaringClass : nodeForRateLimitGroup;
    }
}
