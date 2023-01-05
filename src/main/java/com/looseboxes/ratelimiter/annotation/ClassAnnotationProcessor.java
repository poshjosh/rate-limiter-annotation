package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.Rate;
import com.looseboxes.ratelimiter.annotations.RateGroup;
import com.looseboxes.ratelimiter.node.Node;

import java.lang.reflect.Method;
import java.util.*;

class ClassAnnotationProcessor extends AbstractAnnotationProcessor<Class<?>> {

    private final AnnotationProcessor<Method> methodAnnotationProcessor;

    ClassAnnotationProcessor() {
        this(new MethodAnnotationProcessor());
    }

    ClassAnnotationProcessor(AnnotationProcessor<Method> methodAnnotationProcessor) {
        this.methodAnnotationProcessor = Objects.requireNonNull(methodAnnotationProcessor);
    }

    @Override
    protected Element toElement(Class<?> element) {
        return Element.of(element);
    }

    // We override this here so we can process the class and its super classes
    @Override
    public Node<RateConfig> process(Node<RateConfig> root, NodeConsumer consumer, Class<?> source){
        final List<Element> superClasses = new ArrayList<>();
        final List<Node<RateConfig>> superClassNodes = new ArrayList<>();
        NodeConsumer collectSuperClassNodes = (element, superClassNode) -> {
            if(!superClassNode.isEmpty() && superClasses.contains((Element)element)) {
                superClassNodes.add(superClassNode);
            }
        };

        Node<RateConfig> classNode = null;
        do{

            Node<RateConfig> node = super.doProcess(root, collectSuperClassNodes.andThen(consumer), source);

            final boolean mainNode = classNode == null;

            // If not main node, then it is a super class node, in which case we do not attach
            // the super class node to the root by passing in null as its parent
            // We will transfer all method nodes from each super class node to the main node
            processMethods(mainNode ? root : null, source, consumer);

            if(mainNode) { // The first successfully processed node is the base class
                classNode = node;
            }else{
                if (node != null) {
                    node.getValueOptional().ifPresent(nodeValue -> {
                        superClasses.add((Element)nodeValue.getSource());
                    });
                }
            }

            source = source.getSuperclass();

        }while(source != null && !source.equals(Object.class));

        transferMethodNodesFromSuperClassNodes(classNode, superClassNodes);

        return root;
    }

    private void processMethods(Node<RateConfig> root, Class<?> element, NodeConsumer consumer) {
        Method[] methods = element.getDeclaredMethods();
        methodAnnotationProcessor.processAll(root, consumer, methods);
    }

    /**
     * If class A has 2 super classes B and C both containing resource api endpoint methods, then we transfer
     * those resource api endpoint methods from classes B and C to A.
     * @param classNode The receiving class
     * @param superClassNodes The giving class
     */
    private void transferMethodNodesFromSuperClassNodes(Node<RateConfig> classNode, List<Node<RateConfig>> superClassNodes) {
        if(classNode != null && !superClassNodes.isEmpty()) {

            for(Node<RateConfig> superClassNode : superClassNodes) {

                List<Node<RateConfig>> superClassMethodNodes = superClassNode.getChildren();

                // Transfer method nodes from the super class
                superClassMethodNodes.forEach(node -> node.copyTo(classNode));
            }
        }
    }

    @Override
    protected Node<RateConfig> getOrCreateParent(Node<RateConfig> root, Class<?> element,
                                                   RateGroup rateGroup, Rate[] rates) {
        Node<RateConfig> node = findOrCreateNodeForRateLimitGroupOrNull(root, root, element,
                rateGroup,
                rates);
        return node == null ? root : node;
    }
}
