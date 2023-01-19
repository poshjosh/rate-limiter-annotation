package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.util.Rate;
import io.github.poshjosh.ratelimiter.util.Rates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractAnnotationProcessor
        <G extends GenericDeclaration, A extends Annotation, R extends Rates>
        implements AnnotationProcessor<G>{

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAnnotationProcessor.class);

    private final AnnotationConverter<A, R> annotationConverter;

    protected AbstractAnnotationProcessor(AnnotationConverter<A, R> annotationConverter) {
        this.annotationConverter = Objects.requireNonNull(annotationConverter);
    }

    protected abstract Element toElement(G element);

    protected abstract Node<RateConfig> findExistingParent(Node<RateConfig> root, G element);

    /**
     * @param root the root node
     * @param consumer a consumer that will be applied to each node processed
     * @param element the element for which rate limit annotations will be processed
     * @return The root node
     */
    @Override
    public Node<RateConfig> process(Node<RateConfig> root, NodeConsumer consumer, G element){
        return doProcess(root, findExistingParent(root, element), consumer, element).getRoot();
    }

    /**
     * @return The processed node
     */
    Node<RateConfig> doProcess(Node<RateConfig> root, Node<RateConfig> parent, NodeConsumer consumer, G source){

        final Element element = toElement(source);

        final RateGroup rateGroup = source.getAnnotation(RateGroup.class);

        final A[] rateAnnotations = source.getAnnotationsByType(annotationConverter.getAnnotationType());

        Optional<Node<RateConfig>> existingGroupOptional =
                !isGroupNode(element, rateGroup, rateAnnotations) ? Optional.empty() :
                        findNodeForGroup(root, source, element, rateGroup, rateAnnotations);

        existingGroupOptional.ifPresent(groupNode ->
                requireInitializedOnlyOnce(source, rateGroup, rateAnnotations, groupNode));

        // Call this method before creating the group
        final boolean groupExisted = existingGroupOptional.isPresent();

        Node<RateConfig> createdParent = !isGroupNode(element, rateGroup, rateAnnotations) ? null :
                existingGroupOptional.orElseGet(
                        () -> createNodeForGroup(parent, source, rateGroup, rateAnnotations));

        if (groupExisted && createdParent != null) {
            // Copy existing rates to the newly created parent
            createdParent = copyRatesToNode(root, createdParent, source, element);
            root = createdParent.getRoot(); // Root may have changed
        }

        final Node<RateConfig> parentNode = createdParent == null ? root : createdParent;

        // If the rates have been added to the parent, we do not add them here
        final boolean empty = createdParent != null;
        final Node<RateConfig> node = createNodeForElement(parentNode, source, element, empty);

        LOG.trace("Processed: {} into: {}", element, node);

        consumer.accept(source, node);

        return node;
    }
    private boolean isGroupNode(Element element, RateGroup rateGroup, A [] rates) {
        if (rateGroup == null) {
            return rates.length != 0 && element.isOwnDeclarer();
        }
        return true;
    }
    private Node<RateConfig> copyRatesToNode(
            Node<RateConfig> root, Node<RateConfig> node, G source, Element element) {
        R ratesData = annotationConverter.convert(source);
        RateConfig updated = addCopyOfRatesTo(
                RateConfig.of(element, ratesData), Checks.requireNodeValue(node));
        Node<RateConfig> newRoot = Node.of(root.getName(), root.getValueOrDefault(null));
        Node<RateConfig> newParent = Node.of(node.getName(), updated, newRoot);
        node.getChildren().forEach(child -> child.copyTo(newParent));
        return newParent;
    }
    private RateConfig addCopyOfRatesTo(RateConfig srcConfig, RateConfig tgtConfig) {
        final Rates srcRates = srcConfig.getValue();
        if (!srcRates.hasLimits()) {
            return tgtConfig;
        }
        final Rates tgtRates = tgtConfig.getValue();
        // If we implement [Rates x Rates x...], given that each Rates is [Rate x Rate x...]
        // and x represents the operator. Then we would be able to compose multiple Rates, each
        // with a different operator.
        requireEqualOperator(tgtConfig.getSource(), tgtRates.getOperator(),
                srcConfig.getSource(), srcRates.getOperator());
        if (!tgtRates.hasLimits()) {
            return tgtConfig.withValue(Rates.of(srcRates));
        }
        final Operator operator = Arrays.stream(new Rates[]{tgtRates, srcRates})
                .map(Rates::getOperator)
                .filter(optr -> !Operator.DEFAULT.equals(optr))
                .findAny()
                .orElse(Operator.DEFAULT);
        List<io.github.poshjosh.ratelimiter.util.Rate> composite = new ArrayList<>();
        composite.addAll(tgtRates.getLimits().stream().map(Rate::of).collect(Collectors.toList()));
        composite.addAll(srcRates.getLimits().stream().map(Rate::of).collect(Collectors.toList()));
        return tgtConfig.withValue(Rates.of(operator, composite));
    }

    private Optional<Node<RateConfig>> findNodeForGroup(
            Node<RateConfig> root, G source, Element element, RateGroup rateGroup, A[] rates) {
        final String name = getName(rateGroup, source, rates);
        return root.findFirstChild(childNode -> name.equals(childNode.getName()))
                .map(foundNode -> requireConsistentData(foundNode, element, rateGroup, rates));
    }

    private String getName(RateGroup rateGroup, G source, A [] rateAnnotations) {
        if (rateGroup == null) {
            if (rateAnnotations.length == 0) {
                throw new AssertionError();
            }
            return "group-" + toElement(source).getId();
        }
        return Checks.requireOneContent(
                source, "RateGroup name", rateGroup.name(), rateGroup.value());
    }

    private Node<RateConfig> createNodeForGroup(
            Node<RateConfig> parentNode, G source, RateGroup rateGroup, A[] rateAnnotations) {
        final String groupName = getName(rateGroup, source, rateAnnotations);
        Element element = Element.of(groupName);
        R rates = annotationConverter.convert(source);
        checkRateGroupOperator(rates.getOperator(), rateAnnotations);
        return Node.of(groupName, RateConfig.of(element, rates), parentNode);
    }

    private Node<RateConfig> createNodeForElement(
            Node<RateConfig> parentNode, G source, Element element, boolean empty) {
        // We call this, even when creating an empty instance, it initializes other required fields
        final R rates = annotationConverter.convert(source);
        if (empty) {
            rates.limits();
        }
        return Node.of(element.getId(), RateConfig.of(element, rates), parentNode);
    }

    private void requireInitializedOnlyOnce(
            G source, RateGroup rateGroup, A [] rates, Node<RateConfig> existingGroupNode) {
        if (rateGroup != null && rates.length > 0) {
            existingGroupNode.getValueOptional().ifPresent(rateConfig -> {
                if (rateConfig.getValue().hasLimits()) {
                    throw Checks.exception(
                            "Each RateGroup annotation may be initialized (i.e co-located with Rate annotations) at only one location. For group: " +
                            rateConfig.getSource() + ", found additional at " + source);
                }
            });
        }
   }

    private Node<RateConfig> requireConsistentData(
            Node<RateConfig> rateLimitGroupNode, Element element, RateGroup rateGroup, A[] rates) {
        if(rateGroup != null && rates.length != 0) {
            rateLimitGroupNode.getChildren().stream().map(Checks::requireNodeValue)
                    .forEach(rateConfig -> requireEqualOperator(element, rateGroup, rateConfig));
        }

        return rateLimitGroupNode;
    }

    private void requireEqualOperator(Element src, RateGroup grp, RateConfig cfg) {
        requireEqualOperator(src, operator(grp), cfg.getSource(), cfg.getValue().getOperator());
    }

    private void requireEqualOperator(Object src0, Operator optr0, Object src1, Operator optr1) {
        if (Operator.DEFAULT.equals(optr1) || Operator.DEFAULT.equals(optr0)) {
            return;
        }
        if(!optr1.equals(optr0)) {
            throw Checks.exception("Operator declared at " + src1 + " = " + optr1 +
                    " must match that declared at " + src0 + " = " + optr0);
        }
    }

    private void checkRateGroupOperator(Operator operator, A [] rateAnnotations) {
        if (rateAnnotations.length > 0) {
            return;
        }
        if (!Operator.DEFAULT.equals(operator)) {
            throw Checks.exception(
                    "The operator field may not be specified for a RateGroup when no Rates are co-located with the RateGroup");
        }
    }

    private Operator operator(RateGroup rateGroup) {
        return rateGroup == null ? Operator.DEFAULT : rateGroup.operator();
    }
}
