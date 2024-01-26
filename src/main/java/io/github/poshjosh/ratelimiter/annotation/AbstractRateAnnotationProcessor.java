package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotation.exceptions.AnnotationProcessingException;
import io.github.poshjosh.ratelimiter.annotation.exceptions.DuplicateNameException;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.Rates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.util.*;

abstract class AbstractRateAnnotationProcessor<S extends GenericDeclaration>
        implements RateProcessor<S> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRateAnnotationProcessor.class);

    private final SourceFilter sourceTest;

    private final AnnotationConverter annotationConverter;

    protected AbstractRateAnnotationProcessor(
            SourceFilter sourceTest,
            AnnotationConverter annotationConverter) {
        this.sourceTest = Objects.requireNonNull(sourceTest);
        this.annotationConverter = Objects.requireNonNull(annotationConverter);
    }

    protected abstract RateSource toRateSource(S element);

    /**
     * @param root the root node
     * @param consumer a consumer that will be applied to each node processed
     * @param element the element for which rate limit annotations will be processed
     * @return The root node
     */
    @Override
    public final Node<RateConfig> process(Node<RateConfig> root, NodeConsumer consumer, S element){
        Node<RateConfig> node = doProcess(root, consumer, element);
        return node.isEmptyNode() ? root : node.getRoot(); // The root may have changed
    }

    /**
     * @return The processed node or {@link Node#EMPTY}
     */
    protected Node<RateConfig> doProcess(Node<RateConfig> root, NodeConsumer consumer, S source){
        
        if (!sourceTest.test(source)) {
            LOG.trace("Skipping: {}", source);
            return Node.empty();
        }

        final Node<RateConfig> group = findOrCreateGroup(root, source).orElse(null);

        final Node<RateConfig> parentNode = getParent(root, group, source);

        final Node<RateConfig> node;
        if (isGroupDefinition(source)) {
            node = Node.empty(); // group has been created no need to also create a node
        } else {
            node = createNodeForElement(root, parentNode, source);
        }

        LOG.trace("Processed: {} into:\n{}", source, node);

        consumer.accept(source, node);

        return node;
    }

    protected Node<RateConfig> getParent(Node<RateConfig> root, Node<RateConfig> group, S element) {
        return group == null ? root : group;
    }

    private boolean isGroupDefinition(S source) {
        return source.getAnnotation(RateGroup.class) != null
            || (isAnnotationType(source)
                && source.isAnnotationPresent(annotationConverter.getAnnotationType()));
    }

    private boolean isAnnotationType(S source) {
        return source instanceof Class && ((Class)source).isAnnotation();
    }

    private Optional<Node<RateConfig>> findOrCreateGroup(Node<RateConfig> root, S source) {

        if (isGroupDefinition(source)) {
            return Optional.of(findNodeForGroup(root, source)
                    .orElseGet(() -> createNodeForGroup(root, source)));
        }

        final Optional<Class<? extends Annotation>> groupSourceOptional =
                resolveMetaAnnotationSource(source);

        if (!groupSourceOptional.isPresent()) {
            return Optional.empty();
        }

        final GenericDeclaration groupSource = groupSourceOptional.get();

        return Optional.of(findNodeForGroup(root, groupSource)
                .orElseGet(() -> createNodeForGroup(root, groupSource)));
    }
    private Optional<Class<? extends Annotation>> resolveMetaAnnotationSource(S source) {
        final Rate[] rateAnnotations = source.getAnnotationsByType(annotationConverter.getAnnotationType());
        final Optional<Class<? extends Annotation>> metaAnnotationType = Util.getMetaAnnotationType(
                source, annotationConverter.getAnnotationType());
        if (rateAnnotations.length > 0 && metaAnnotationType.isPresent()) {
            throw new AnnotationProcessingException(
                    "RateSource may not be annotated with @Rate both directly and indirectly (via meta annotation): " + source);
        }
        return metaAnnotationType;
    }

    private Optional<Node<RateConfig>> findNodeForGroup(
            Node<RateConfig> root, GenericDeclaration groupSource) {
        final String groupName = RateId.of((Class<?>)groupSource);
        return root.findFirstChild(childNode -> groupName.equals(childNode.getName()));
    }
    private Node<RateConfig> createNodeForGroup(
            Node<RateConfig> root, GenericDeclaration groupSource) {
        final Class<?> clazz = (Class<?>)groupSource;
        final RateSource rateSource = JavaRateSource.ofAnnotation(clazz);
        final Rates rates = annotationConverter.convert(rateSource);
        checkRateGroupOperator(rates.getOperator(), rates);
        final RateConfig rootConfig = root.getValueOrDefault(null);
        return Node.of(rateSource.getId(), RateConfig.of(rateSource, rates, rootConfig), root);
    }

    private Node<RateConfig> createNodeForElement(
            Node<RateConfig> root, Node<RateConfig> parentNode, S source) {
        final RateSource rateSource = toRateSource(source);
        requireUniqueName(root, source, rateSource.getId());
        final Rates rates = annotationConverter.convert(rateSource);
        final RateConfig parentConfig = parentNode.getValueOrDefault(null);
        return Node.of(rateSource.getId(), RateConfig.of(rateSource, rates, parentConfig), parentNode);
    }

    private String requireUniqueName(Node<RateConfig> root, Object source, String name) {
        root.findFirstChild(child -> Objects.equals(name, child.getName()))
                .ifPresent(child -> {
                    final Object existingSource = child.getValueOptional()
                            .map(RateConfig::getSource).orElse(null);
                    throw new DuplicateNameException(name, existingSource, source);
                });
        return name;
    }

    private void checkRateGroupOperator(Operator operator, Rates rates) {
        if (rates.hasLimitsSet()) {
            return;
        }
        if (!Operator.NONE.equals(operator)) {
            throw Checks.exception(
                    "The operator field may not be specified for a RateGroup when no Rates are co-located with the RateGroup");
        }
    }
}
