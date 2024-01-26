package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotation.exceptions.AnnotationProcessingException;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

class MethodRateAnnotationProcessor extends AbstractRateAnnotationProcessor<Method> {

    MethodRateAnnotationProcessor(AnnotationConverter annotationConverter) {
        this(RateProcessor.SourceFilter.ofRateLimited(), annotationConverter);
    }
    
    MethodRateAnnotationProcessor(
            SourceFilter sourceTest,
            AnnotationConverter annotationConverter) {
        super(sourceTest, annotationConverter);
    }

    @Override protected RateSource toRateSource(Method element) {
        return JavaRateSource.of(element);
    }

    @Override protected Node<RateConfig> getParent(
            Node<RateConfig> root, Node<RateConfig> group, Method method) {
        Node<RateConfig> declaring = getDeclaringClassNode(root, method).orElse(null);
        if (group == null) {
            return declaring;
        }
        if (declaring == null) {
            return group;
        }
        if (hasLimits(declaring)) { // group != null && declaring class has limits specified
            throw new AnnotationProcessingException(
                    "Methods of a class with rates defined at the class level, may not belong to a group. Method " +
                    method + ", group: " + source(group));
        }
        return group;
    }

    private Object source(Node<RateConfig> node) {
        return node.getValueOptional().map(RateConfig::getSource).orElse(null);
    }

    private boolean hasLimits(Node<RateConfig> node) {
        RateConfig rateConfig = node == null ? null : node.getValueOrDefault(null);
        if (rateConfig == null) {
            return false;
        }
        return rateConfig.getRates().hasLimitsSet();
    }

    private Optional<Node<RateConfig>> getDeclaringClassNode(Node<RateConfig> root, Method method) {
        final String declaringClassId = RateId.of(method.getDeclaringClass());
        Predicate<Node<RateConfig>> testForDeclaringClass = node -> {
            RateConfig rateConfig = node.getValueOrDefault(null);
            if (rateConfig == null) {
                return false;
            }
            return declaringClassId.equals((rateConfig.getSource()).getId());
        };
        return root.findFirstChild(testForDeclaringClass);
    }
}
