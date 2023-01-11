package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.NodeFormatter;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.util.Rates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericDeclaration;
import java.util.*;

public abstract class AbstractAnnotationProcessor
        <S extends GenericDeclaration, I extends Annotation, O extends Rates>
        implements AnnotationProcessor<S>{

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAnnotationProcessor.class);

    private final AnnotationConverter<I, O> annotationConverter;

    protected AbstractAnnotationProcessor(AnnotationConverter<I, O> annotationConverter) {
        this.annotationConverter = Objects.requireNonNull(annotationConverter);
    }

    protected abstract Node<RateConfig> getOrCreateParent(
            Node<RateConfig> root, S element,
            RateGroup rateGroup, I[] rates);

    protected abstract Element toElement(S element);

    @Override
    public Node<RateConfig> process(Node<RateConfig> root, NodeConsumer consumer, S element){
        doProcess(root, consumer, element);
        return root;
    }

    /**
     * @return The processed node
     */
    protected Node<RateConfig> doProcess(Node<RateConfig> root, NodeConsumer consumer, S source){

        final I[] rates = source.getAnnotationsByType(annotationConverter.getAnnotationType());

        final Node<RateConfig> node;

        final Element element = toElement(source);

        if(rates.length > 0 ) {

            RateGroup rateGroup = source.getAnnotation(RateGroup.class);
            Node<RateConfig> createdParent = getOrCreateParent(root, source, rateGroup, rates);

            Node<RateConfig> parentNode = createdParent == null ? root : createdParent;
            String name = element.getId();
            node = createNodeForElementOrNull(parentNode, name, element, rateGroup, rates);

        }else{
            node = Node.empty();
        }

        if(LOG.isTraceEnabled()) {
            LOG.trace("\nProcessed: {}\nInto Node: {}", element, NodeFormatter.indented().format(node));
        }

        consumer.accept(element, node);

        return node;
    }

    protected Node<RateConfig> findOrCreateNodeForRateLimitGroupOrNull(
            Node<RateConfig> root, Node<RateConfig> parent,
            GenericDeclaration annotatedElement, RateGroup rateGroup, I[] rates) {
        String name = getName(rateGroup);
        final Node<RateConfig> node;
        if(root == null || rateGroup == null || name.isEmpty()) {
            node = null;
        }else{
            node = root.findFirstChild(childNode -> name.equals(childNode.getName()))
                    .map(foundNode -> requireConsistentData(foundNode, annotatedElement, rateGroup,
                            rates))
                    .orElseGet(() -> createNodeForGroupOrNull(parent, name, rateGroup, rates));
        }

        return node;
    }

    private String getName(RateGroup rateGroup) {
        return rateGroup == null ? "" : selectFirstValidOrEmptyText(rateGroup.name(), rateGroup.value());
    }

    private String selectFirstValidOrEmptyText(String ...candidates) {
        for(String candidate : candidates) {
            if(candidate != null && !candidate.isEmpty()) {
                return candidate;
            }
        }
        return "";
    }

    private Node<RateConfig> createNodeForGroupOrNull(
            Node<RateConfig> parentNode, String name,
            RateGroup rateGroup, I[] rateAnnotations) {
        if(rateAnnotations.length == 0) {
            return null;
        }else{
            Element element = Element.of(name);
            O rates = annotationConverter.convert(rateGroup, element,
                    (I[])Array.newInstance(annotationConverter.getAnnotationType(), 0));
            return Node.of(name, RateConfig.of(element, rates), parentNode);
        }
    }

    protected Node<RateConfig> createNodeForElementOrNull(
            Node<RateConfig> parentNode, String name, Element element,
            RateGroup rateGroup, I[] rateAnnotations) {
        if(rateAnnotations.length == 0) {
            return Node.empty();
        }else{
            final O rates = annotationConverter.convert(rateGroup, element, rateAnnotations);
            return Node.of(name, RateConfig.of(element, rates), parentNode);
        }
    }

    private Node<RateConfig> requireConsistentData(
            Node<RateConfig> rateLimitGroupNode, GenericDeclaration annotatedElement,
            RateGroup rateGroup, I[] rates) {
        if(rateGroup != null && rates.length != 0) {
            final Operator operator = operator(rateGroup);
            rateLimitGroupNode.getChildren().stream()
                    .map(childNode -> childNode.getValueOptional()
                            .orElseThrow(() -> new AnnotationProcessingException("Only the root node may have no value")))
                    .map(RateConfig::getValue)
                    .forEach(existing -> requireEqual(annotatedElement, rateGroup, operator, existing));
        }

        return rateLimitGroupNode;
    }

    private void requireEqual(GenericDeclaration annotatedElement,
            RateGroup rateGroup, Operator lhs, Rates existing) {
        if(!existing.getOperator().equals(lhs)) {
            throw new AnnotationProcessingException("Found inconsistent operator, for "
                    + rateGroup + " declared at " + annotatedElement);
        }
    }

    private Operator operator(RateGroup rateGroup) {
        return rateGroup == null ? AnnotationProcessor.DEFAULT_OPERATOR : rateGroup.operator();
    }
}
