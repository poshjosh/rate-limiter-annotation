package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.util.Rates;

import java.lang.reflect.Method;
import java.util.*;

class ClassRateAnnotationProcessor extends AbstractRateAnnotationProcessor<Class<?>, Rates> {

    private final RateProcessor<Method> methodRateProcessor;

    ClassRateAnnotationProcessor(
            SourceFilter sourceTest, AnnotationConverter<Rate, Rates> annotationConverter) {
        this(sourceTest, annotationConverter, 
                new MethodRateAnnotationProcessor(sourceTest, annotationConverter));
    }

    ClassRateAnnotationProcessor(
            SourceFilter sourceTest,
            AnnotationConverter<Rate, Rates> annotationConverter,
            RateProcessor<Method> methodRateProcessor) {
        super(sourceTest, annotationConverter);
        this.methodRateProcessor = methodRateProcessor;
    }

    @Override
    protected RateSource toRateSource(Class<?> element) {
        return RateSourceFactory.of(element);
    }

    // We override this here so we can process the class and its super classes
    @Override
    protected Node<RateConfig> doProcess(Node<RateConfig> root, NodeConsumer consumer, Class<?> source){
        final List<Class<?>> superClasses = new ArrayList<>();
        final List<Node<RateConfig>> superClassNodes = new ArrayList<>();
        NodeConsumer collectSuperClassNodes = (sourceClass, superClassNode) -> {
            if(!superClassNode.isEmptyNode() && superClasses.contains((Class<?>)sourceClass)) {
                superClassNodes.add(superClassNode);
            }
        };

        Node<RateConfig> classNode = null;
        do{

            final Node<RateConfig> node = super.doProcess(root, collectSuperClassNodes.andThen(consumer), source);

            if (!node.isEmptyNode()) {
                root = node.getRoot(); // The root may have changed
            }

            final boolean initialState = classNode == null;

            root = processMethods(root, source, consumer);

            if(initialState) { // The first successfully processed node is the base class
                classNode = node;
            }else{
                final Class<?> finalReference = source;
                node.getValueOptional().ifPresent(nodeValue -> {
                    superClasses.add(finalReference);
                });
            }

            source = source.getSuperclass();

        }while(source != null && !source.equals(Object.class));

        transferMethodNodesFromSuperClassNodes(classNode, superClassNodes);

        return classNode;
    }

    private Node<RateConfig> processMethods(Node<RateConfig> root, Class<?> element, NodeConsumer consumer) {
        Method[] methods = element.getDeclaredMethods();
        return methodRateProcessor.processAll(root, consumer, methods);
    }

    /**
     * If class A has 2 super classes B and C both host Rate related annotations, then we transfer
     * the date resolved from those Rate related annotations from classes B and C to A.
     * @param classNode The receiving class
     * @param superClassNodes The giving class
     */
    private void transferMethodNodesFromSuperClassNodes(Node<RateConfig> classNode, List<Node<RateConfig>> superClassNodes) {
        if(classNode != null && !superClassNodes.isEmpty()) {

            for(Node<RateConfig> superClassNode : superClassNodes) {

                List<Node<RateConfig>> superClassMethodNodes = superClassNode.getChildren();

                // Transfer method nodes from the super class
                superClassMethodNodes.forEach(node -> {
                    node.copyTo(classNode);
                });
            }
        }
    }
}
