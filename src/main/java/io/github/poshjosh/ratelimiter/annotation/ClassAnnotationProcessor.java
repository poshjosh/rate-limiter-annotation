package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Rates;

import java.lang.reflect.Method;
import java.util.*;

class ClassAnnotationProcessor extends AbstractAnnotationProcessor<Class<?>, Rate, Rates>{

    private final AnnotationProcessor<Method> methodAnnotationProcessor;

    ClassAnnotationProcessor(AnnotationConverter<Rate, Rates> annotationConverter) {
        this(new MethodAnnotationProcessor(annotationConverter), annotationConverter);
    }

    ClassAnnotationProcessor(
            AnnotationProcessor<Method> methodAnnotationProcessor,
            AnnotationConverter<Rate, Rates> annotationConverter) {
        super(annotationConverter);
        this.methodAnnotationProcessor = Objects.requireNonNull(methodAnnotationProcessor);
    }

    @Override
    protected Element toElement(Class<?> element) {
        return Element.of(element);
    }

    @Override
    protected Node<RateConfig> findExistingParent(Node<RateConfig> root, Class<?> element) {
        return root;
    }

    // We override this here so we can process the class and its super classes
    @Override
    public Node<RateConfig> process(Node<RateConfig> root, NodeConsumer consumer, Class<?> source){
        final List<Class<?>> superClasses = new ArrayList<>();
        final List<Node<RateConfig>> superClassNodes = new ArrayList<>();
        NodeConsumer collectSuperClassNodes = (sourceClass, superClassNode) -> {
            if(!superClassNode.isEmptyNode() && superClasses.contains((Class<?>)sourceClass)) {
                superClassNodes.add(superClassNode);
            }
        };

        Node<RateConfig> classNode = null;
        do{

            Node<RateConfig> parent = findExistingParent(root, source);

            Node<RateConfig> node = super.doProcess(root, parent, collectSuperClassNodes.andThen(consumer), source);

            root = node.getRoot(); // The root may have changed

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

        return root;
    }

    private Node<RateConfig> processMethods(Node<RateConfig> root, Class<?> element, NodeConsumer consumer) {
        Method[] methods = element.getDeclaredMethods();
        return methodAnnotationProcessor.processAll(root, consumer, methods);
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
