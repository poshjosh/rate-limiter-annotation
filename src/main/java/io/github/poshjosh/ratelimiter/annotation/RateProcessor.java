package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.Rates;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

public interface RateProcessor<S> {
    
    @FunctionalInterface
    interface SourceFilter extends Predicate<GenericDeclaration> {
        static SourceFilter ofRateLimited() {
            return new RateLimitedSourceTest();
        }
    }
    
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

    static RateProcessor<Class<?>> ofDefaults() {
        return ofClass();
    }

    static RateProcessor<Class<?>> ofClass() {
        return ofClass(RateProcessor.SourceFilter.ofRateLimited());
    }

    static RateProcessor<Method> ofMethod() {
        return ofMethod(RateProcessor.SourceFilter.ofRateLimited());
    }

    static RateProcessor<Class<?>> ofClass(SourceFilter sourceTest) {
        return ofClass(sourceTest, AnnotationConverter.ofRate());
    }

    static RateProcessor<Method> ofMethod(SourceFilter sourceTest) {
        return ofMethod(sourceTest, AnnotationConverter.ofRate());
    }

    static RateProcessor<Class<?>> ofClass(
            SourceFilter sourceTest, 
            AnnotationConverter<Rate, Rates> annotationConverter) {
        return new ClassRateAnnotationProcessor(sourceTest, annotationConverter);
    }

    static RateProcessor<Method> ofMethod(
            SourceFilter sourceTest,
            AnnotationConverter<Rate, Rates> annotationConverter) {
        return new MethodRateAnnotationProcessor(sourceTest, annotationConverter);
    }

    default Node<RateConfig> processAll(S... sources) {
        return processAll(new HashSet<>(Arrays.asList(sources))).getRoot();
    }

    default Node<RateConfig> processAll(Set<S> sources) {
        return processAll(Node.ofDefaultRoot(), (src, node) -> {}, sources).getRoot();
    }

    default Node<RateConfig> processAll(Node<RateConfig> root, NodeConsumer consumer, S... sources) {
        return processAll(root, consumer, new HashSet<>(Arrays.asList(sources))).getRoot();
    }

    default Node<RateConfig> processAll(Node<RateConfig> root, NodeConsumer consumer, Set<S> sources) {
        for(S source : sources) {
            root = process(root, consumer, source);
        }
        return root.getRoot();
    }

    default Node<RateConfig> process(S source) {
        return process(Node.ofDefaultRoot(), (src, node) -> { }, source).getRoot();
    }

    /**
     * @param root the root node
     * @param consumer a consumer that will be applied to each node processed
     * @param source the source for which rate limit annotations will be processed
     * @return The root node
     */
    Node<RateConfig> process(Node<RateConfig> root, NodeConsumer consumer, S source);
}
