package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.RateLimit;
import com.looseboxes.ratelimiter.annotations.RateLimitGroup;
import com.looseboxes.ratelimiter.node.Node;

import java.lang.reflect.Method;
import java.util.*;

final class ClassAnnotationProcessor<T> extends AbstractAnnotationProcessor<Class<?>, T> {

    private final AnnotationProcessor<Method, T> methodAnnotationProcessor;

    ClassAnnotationProcessor(
            IdProvider<Class<?>, String> idProvider,
            AnnotationProcessor.Converter<T> converter,
            AnnotationProcessor<Method, T> methodAnnotationProcessor) {
        super(idProvider, converter);
        this.methodAnnotationProcessor = Objects.requireNonNull(methodAnnotationProcessor);
    }

    // We override this here so we can process the class and its super classes
    @Override
    public Node<NodeValue<T>> process(Node<NodeValue<T>> root, NodeConsumer<T> consumer, Class<?> element){
        final List<Class<?>> superClasses = new ArrayList<>();
        final List<Node<NodeValue<T>>> superClassNodes = new ArrayList<>();
        NodeConsumer<T> collectSuperClassNodes = (source, superClassNode) -> {
            if(superClasses.contains((Class<?>)source)) {
                superClassNodes.add(superClassNode);
            }
        };

        Node<NodeValue<T>> classNode = null;
        do{

            Node<NodeValue<T>> node = super.doProcess(root, collectSuperClassNodes.andThen(consumer), element);

            final boolean mainNode = classNode == null;

            // If not main node, then it is a super class node, in which case we do not attach
            // the super class node to the root by passing in null as its parent
            // We will transfer all method nodes from each super class node to the main node
            processMethods(mainNode ? root : null, element, consumer);

            if(mainNode) { // The first successfully processed node is the base class
                classNode = node;
            }else{
                superClasses.add(element);
            }

            element = element.getSuperclass();

        }while(element != null && !element.equals(Object.class));

        transferMethodNodesFromSuperClassNodes(classNode, superClassNodes);

        return root;
    }

    private void processMethods(Node<NodeValue<T>> root, Class<?> element, NodeConsumer<T> consumer) {
        Method[] methods = element.getDeclaredMethods();
        methodAnnotationProcessor.processAll(root, consumer, methods);
    }

    /**
     * If class A has 2 super classes B and C both containing resource api endpoint methods, then we transfer
     * those resource api endpoint methods from classes B and C to A.
     * @param classNode The receiving class
     * @param superClassNodes The giving class
     */
    private void transferMethodNodesFromSuperClassNodes(Node<NodeValue<T>> classNode, List<Node<NodeValue<T>>> superClassNodes) {
        if(classNode != null && !superClassNodes.isEmpty()) {

            for(Node<NodeValue<T>> superClassNode : superClassNodes) {

                List<Node<NodeValue<T>>> superClassMethodNodes = superClassNode.getChildren();

                // Transfer method nodes from the super class
                superClassMethodNodes.forEach(node -> node.copyTo(classNode));
            }
        }
    }

    @Override
    protected Node<NodeValue<T>> getOrCreateParent(Node<NodeValue<T>> root, Class<?> element,
                                                   RateLimitGroup rateLimitGroup, RateLimit[] rateLimits) {
        Node<NodeValue<T>> node = findOrCreateNodeForRateLimitGroupOrNull(root, root, element, rateLimitGroup, rateLimits);
        return node == null ? root : node;
    }
}
