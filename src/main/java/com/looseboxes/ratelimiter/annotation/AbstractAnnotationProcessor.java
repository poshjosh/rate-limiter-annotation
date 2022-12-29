package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.RateLimit;
import com.looseboxes.ratelimiter.annotations.RateLimitGroup;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.NodeFormatter;
import com.looseboxes.ratelimiter.util.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.GenericDeclaration;
import java.util.Objects;

public abstract class AbstractAnnotationProcessor<S extends GenericDeclaration, T>
        implements AnnotationProcessor<S, T>{

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAnnotationProcessor.class);

    private static final Object sourceForGroupNodes = new Object();

    private final IdProvider<S, String> idProvider;

    private final Converter<T> converter;

    protected AbstractAnnotationProcessor(IdProvider<S, String> idProvider, Converter<T> converter) {
        this.idProvider = Objects.requireNonNull(idProvider);
        this.converter = Objects.requireNonNull(converter);
    }

    protected abstract Node<NodeValue<T>> getOrCreateParent(
            Node<NodeValue<T>> root, S element,
            RateLimitGroup rateLimitGroup, RateLimit[] rateLimits);

    @Override
    public Node<NodeValue<T>> process(Node<NodeValue<T>> root, NodeConsumer<T> consumer, S element){
        doProcess(root, consumer, element);
        return root;
    }

    /**
     * @return The processed node
     */
    protected Node<NodeValue<T>> doProcess(Node<NodeValue<T>> root, NodeConsumer<T> consumer, S element){

        final RateLimit [] rateLimits = element.getAnnotationsByType(RateLimit.class);

        final Node<NodeValue<T>> node;

        if(rateLimits.length > 0 ) {

            RateLimitGroup rateLimitGroup = element.getAnnotation(RateLimitGroup.class);
            Node<NodeValue<T>> created = getOrCreateParent(root, element, rateLimitGroup, rateLimits);

            Node<NodeValue<T>> parentNode = created == null ? root : created;
            String name = idProvider.getId(element);
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
        return Node.of(name, NodeValue.of(sourceForGroupNodes, value), parent);
    }

    protected Node<NodeValue<T>> createNodeForElementOrNull(
            Node<NodeValue<T>> parentNode, String name, Object element,
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
