package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.NodeFormatter;
import io.github.poshjosh.ratelimiter.util.Rates;
import io.github.poshjosh.ratelimiter.util.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.GenericDeclaration;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class AbstractAnnotationProcessor<S extends GenericDeclaration>
        implements AnnotationProcessor<S>{

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAnnotationProcessor.class);

    protected abstract Node<RateConfig> getOrCreateParent(
            Node<RateConfig> root, S element,
            RateGroup rateGroup, Rate[] rates);

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

        final Rate[] rates = source.getAnnotationsByType(Rate.class);

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
            GenericDeclaration annotatedElement, RateGroup rateGroup, Rate[] rates) {
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
            RateGroup rateGroup, Rate[] rates) {
        if(rates.length == 0) {
            return null;
        }else{
            return createGroupNode(parentNode, name, process(rateGroup));
        }
    }

    private Node<RateConfig> createGroupNode(Node<RateConfig> parent, String name, Rates value) {
        return Node.of(name, RateConfig.of(Element.of(name), value), parent);
    }

    protected Node<RateConfig> createNodeForElementOrNull(
            Node<RateConfig> parentNode, String name, Element element,
            RateGroup rateGroup, Rate[] rateLimits) {
        if(rateLimits.length == 0) {
            return Node.empty();
        }else{
            final Rates rates = convert(rateGroup, rateLimits);
            return Node.of(name, RateConfig.of(element, rates), parentNode);
        }
    }

    private Node<RateConfig> requireConsistentData(
            Node<RateConfig> rateLimitGroupNode, GenericDeclaration annotatedElement,
            RateGroup rateGroup, Rate[] rates) {
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
            throw new AnnotationProcessingException("Found inconsistent operator, for " + rateGroup
                    + " declared at " + annotatedElement);
        }
    }

    private Rates process(RateGroup rateGroup) {
        return convert(rateGroup, new Rate[0]);
    }

    private Rates convert(RateGroup rateGroup, Rate[] rates) {
        final Operator operator = operator(rateGroup);
        if (rates.length == 0) {
            return Rates.of(operator);
        }
        final io.github.poshjosh.ratelimiter.util.Rate[] configs = new io.github.poshjosh.ratelimiter.util.Rate[rates.length];
        for (int i = 0; i < rates.length; i++) {
            configs[i] = createRate(rates[i]);
        }
        return Rates.of(operator, configs);
    }

    private Operator operator(RateGroup rateGroup) {
        return rateGroup == null ? AnnotationProcessor.DEFAULT_OPERATOR : rateGroup.operator();
    }

    private io.github.poshjosh.ratelimiter.util.Rate createRate(Rate rate) {
        Duration duration = Duration.of(rate.duration(), toChronoUnit(rate.timeUnit()));
        return io.github.poshjosh.ratelimiter.util.Rate.of(rate.permits(), duration, rate.factoryClass());
    }

    private ChronoUnit toChronoUnit(TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit);
        if(TimeUnit.NANOSECONDS.equals(timeUnit)) {
            return ChronoUnit.NANOS;
        }
        if(TimeUnit.MICROSECONDS.equals(timeUnit)) {
            return ChronoUnit.MICROS;
        }
        if(TimeUnit.MILLISECONDS.equals(timeUnit)) {
            return ChronoUnit.MILLIS;
        }
        if(TimeUnit.SECONDS.equals(timeUnit)) {
            return ChronoUnit.SECONDS;
        }
        if(TimeUnit.MINUTES.equals(timeUnit)) {
            return ChronoUnit.MINUTES;
        }
        if(TimeUnit.HOURS.equals(timeUnit)) {
            return ChronoUnit.HOURS;
        }
        if(TimeUnit.DAYS.equals(timeUnit)) {
            return ChronoUnit.DAYS;
        }
        throw new IllegalArgumentException("Unexpected TimeUnit: " + timeUnit);
    }
}
