package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.RateLimit;
import com.looseboxes.ratelimiter.annotations.RateLimitGroup;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.NodeFormatter;
import com.looseboxes.ratelimiter.util.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.GenericDeclaration;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractAnnotationProcessor<S extends GenericDeclaration, T>
        implements AnnotationProcessor<S, T>{

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAnnotationProcessor.class);

    private final Converter<T> converter;

    protected AbstractAnnotationProcessor(Converter<T> converter) {
        this.converter = Objects.requireNonNull(converter);
    }

    protected abstract Node<NodeValue<T>> getOrCreateParent(
            Node<NodeValue<T>> root, S element,
            RateLimitGroup rateLimitGroup, RateLimit[] rateLimits);

    protected abstract Element toElement(String id, S element);

    @Override
    public Node<NodeValue<T>> process(Node<NodeValue<T>> root, NodeConsumer<T> consumer, S element){
        doProcess(root, consumer, element);
        return root;
    }

    /**
     * @return The processed node
     */
    protected Node<NodeValue<T>> doProcess(Node<NodeValue<T>> root, NodeConsumer<T> consumer, S source){

        final RateLimit [] rateLimits = source.getAnnotationsByType(RateLimit.class);

        final Node<NodeValue<T>> node;

        final Element element = toElement(getName(rateLimits, source), source);

        if(rateLimits.length > 0 ) {

            RateLimitGroup rateLimitGroup = source.getAnnotation(RateLimitGroup.class);
            Node<NodeValue<T>> createdParent = getOrCreateParent(root, source, rateLimitGroup, rateLimits);

            Node<NodeValue<T>> parentNode = createdParent == null ? root : createdParent;
            String name = element.getId();
            node = createNodeForElementOrNull(parentNode, name, element, rateLimitGroup, rateLimits);

        }else{
            node = null;
        }

        if(LOG.isTraceEnabled()) {
            LOG.trace("\nProcessed: {}\nInto Node: {}", element, NodeFormatter.indented().format(node));
        }

        consumer.accept(element, node);

        return node;
    }

    private String getName(RateLimit[] rateLimits, S source) {
        if (rateLimits == null || rateLimits.length == 0) {
            return "";
        }
        if (rateLimits.length == 1) {
            return rateLimits[0].name();
        }
        return requireSameName(rateLimits, source);
    }

    private String requireSameName(RateLimit[] rateLimits, S source) {
        Set<String> uniqueNames = Arrays.stream(rateLimits)
                .map(RateLimit::name).collect(Collectors.toSet());
        if (uniqueNames.size() > 1) {
            throw new AnnotationProcessingException(
                    "Multiple " + RateLimit.class.getSimpleName() +
                    " annotations on a single node must resolve to only one unique name, found: " +
                    uniqueNames + " at " + source);

        }
        return uniqueNames.iterator().next();
    }

    protected Node<NodeValue<T>> findOrCreateNodeForRateLimitGroupOrNull(
            Node<NodeValue<T>> root, Node<NodeValue<T>> parent,
            GenericDeclaration annotatedElement, RateLimitGroup rateLimitGroup, RateLimit [] rateLimits) {
        String name = getName(rateLimitGroup);
        final Node<NodeValue<T>> node;
        if(root == null || rateLimitGroup == null || name.isEmpty()) {
            node = null;
        }else{
            node = root.findFirstChild(n -> name.equals(n.getName()))
                    .map(foundNode -> requireConsistentData(foundNode, annotatedElement, rateLimitGroup, rateLimits))
                    .orElseGet(() -> createNodeForGroupOrNull(parent, name, rateLimitGroup, rateLimits));
        }

        return node;
    }

    private String getName(RateLimitGroup rateLimitGroup) {
        return rateLimitGroup == null ? "" : selectFirstValidOrEmptyText(rateLimitGroup.name(), rateLimitGroup.value());
    }

    private String selectFirstValidOrEmptyText(String ...candidates) {
        for(String candidate : candidates) {
            if(candidate != null && !candidate.isEmpty()) {
                return candidate;
            }
        }
        return "";
    }

    private Node<NodeValue<T>> createNodeForGroupOrNull(
            Node<NodeValue<T>> parentNode, String name,
            RateLimitGroup rateLimitGroup, RateLimit [] rateLimits) {
        if(rateLimits.length == 0) {
            return null;
        }else{
            return createGroupNode(parentNode, name, process(rateLimitGroup));
        }
    }

    private Node<NodeValue<T>> createGroupNode(Node<NodeValue<T>> parent, String name, T value) {
        return Node.of(name, NodeValue.of(Element.of(name), value), parent);
    }

    protected Node<NodeValue<T>> createNodeForElementOrNull(
            Node<NodeValue<T>> parentNode, String name, Element element,
            RateLimitGroup rateLimitGroup, RateLimit [] rateLimits) {
        if(rateLimits.length == 0) {
            return null;
        }else{
            T limit = converter.convert(rateLimitGroup, rateLimits);
            return Node.of(name, NodeValue.of(element, limit), parentNode);
        }
    }

    private Node<NodeValue<T>> requireConsistentData(
            Node<NodeValue<T>> rateLimitGroupNode, GenericDeclaration annotatedElement,
            RateLimitGroup rateLimitGroup, RateLimit [] rateLimits) {
        if(rateLimitGroup != null && rateLimits.length != 0) {
            final Operator operator = operator(rateLimitGroup);
            rateLimitGroupNode.getChildren().stream()
                    .map(childNode -> childNode.getValueOptional()
                            .orElseThrow(() -> new AnnotationProcessingException("Only the root node may have no value")))
                    .map(NodeValue::getValue)
                    .forEach(existing -> requireEqual(annotatedElement, rateLimitGroup, operator, existing));
        }

        return rateLimitGroupNode;
    }

    private Operator operator(RateLimitGroup rateLimitGroup) {
        return rateLimitGroup == null ? AnnotationProcessor.DEFAULT_OPERATOR : rateLimitGroup.operator();
    }

    private void requireEqual(GenericDeclaration annotatedElement, RateLimitGroup rateLimitGroup, Operator lhs, T existing) {
        if(!converter.isOperatorEqual(existing, lhs)) {
            throw new AnnotationProcessingException("Found inconsistent operator, for " +
                    rateLimitGroup + " declared at " + annotatedElement);
        }
    }

    private T process(RateLimitGroup rateLimitGroup) {
        return converter.convert(rateLimitGroup, new RateLimit[0]);
    }
}
