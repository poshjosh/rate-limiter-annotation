package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.annotation.RateProcessors;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.RateSources;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

class RootNodes<K> {

    private static final Logger LOG = LoggerFactory.getLogger(RootNodes.class);

    static <K> RootNodes<K> of(RateLimiterContext<K> context) {
        return new RootNodes<>(context);
    }
    static <K> RootNodes<K> of(Node<MatchContext<K>> node) {
        return new RootNodes<>(node, Node.empty());
    }

    private final Node<MatchContext<K>> propertiesRootNode;
    private final Node<MatchContext<K>> annotationsRootNode;

    private RootNodes(RateLimiterContext<K> context) {
        final RateConfigCollector propertyConfigs = new RateConfigCollector();
        Node<RateConfig> propRoot = getPropertyRateProcessor()
                .process(createRootNode("root.properties"),
                        propertyConfigs, context.getProperties());

        Node<RateConfig> annoRoot = getClassRateProcessor()
                .processAll(createRootNode("root.annotations"),
                        (src, node) -> {}, context.getTargetClasses());

        final List<String> transferredToAnnotations = new ArrayList<>();
        Function<Node<RateConfig>, RateConfig> overrideWithPropertyValue = node -> {
            if (node.isRoot()) {
                return node.getValueOrDefault(null);
            }
            final RateConfig annotationConfig = node.requireValue();
            final RateConfig propertyConfig = propertyConfigs.getOrNull(node.getName());
            if (propertyConfig == null) {
                return annotationConfig;
            }
            transferredToAnnotations.add(node.getName());
            return propertyConfig.withSource(annotationConfig.getSource());
        };

        annoRoot = annoRoot.transform(overrideWithPropertyValue);

        Predicate<Node<RateConfig>> isNodeRateLimited = node -> {
            if (node.isRoot()) {
                return true;
            }
            if (transferredToAnnotations.contains(node.getName())) {
                return true;
            }
            final RateConfig nodeValue = node.getValueOrDefault(null);
            if (nodeValue == null) {
                return false;
            }
            return nodeValue.getSource().isRateLimited();
        };

        Predicate<Node<RateConfig>> anyNodeInTreeIsRateLimited =
                node -> node.anyMatch(isNodeRateLimited);

        Function<Node<RateConfig>, MatchContext<K>> transformer = currentNode ->
                MatchContexts.of(context.getMatcherProvider(), currentNode);

        annotationsRootNode = annoRoot.retainAll(anyNodeInTreeIsRateLimited)
                .orElseGet(() -> Nodes.of("root.annotations"))
                .getRoot().transform(transformer);

        LOG.debug("ANNOTATION SOURCED NODES:\n{}", annotationsRootNode);

        Predicate<Node<RateConfig>> nodesNotTransferred =
                node -> !transferredToAnnotations.contains(node.getName());

        propertiesRootNode = propRoot.retainAll(nodesNotTransferred)
                .orElseGet(() -> Nodes.of("root.properties"))
                .getRoot().transform(transformer);

        LOG.debug("PROPERTIES SOURCED NODES:\n{}", propertiesRootNode);
    }

    private RootNodes(Node<MatchContext<K>> propertiesRootNode,
            Node<MatchContext<K>> annotationsRootNode) {
        this.propertiesRootNode = Objects.requireNonNull(propertiesRootNode);
        this.annotationsRootNode = Objects.requireNonNull(annotationsRootNode);
    }

    private Node<RateConfig> createRootNode(String id) {
        final RateSource rateSource = RateSources.of(id);
        return Nodes.of(id, RateConfig.of(rateSource, Rates.none()));
    }

    public boolean hasProperties() {
        // TODO - Find out why #hasChildren() led to x100 increase in memory usage over #size()
//        return !propertiesRootNode.isEmptyNode() && propertiesRootNode.hasChildren();
        return !propertiesRootNode.isEmptyNode() && propertiesRootNode.size() > 0;
    }

    public boolean hasAnnotations() {
        // TODO - Find out why #hasChildren() led to x100 increase in memory usage over #size()
//        return !annotationsRootNode.isEmptyNode() && annotationsRootNode.hasChildren();
        return !annotationsRootNode.isEmptyNode() && annotationsRootNode.size() > 0;
    }

    public Node<MatchContext<K>> getPropertiesRootNode() {
        return propertiesRootNode;
    }

    public Node<MatchContext<K>> getAnnotationsRootNode() {
        return annotationsRootNode;
    }

    private RateProcessor<Class<?>> getClassRateProcessor() {
        // We accept all class/method  nodes, even those without rate limit related annotations
        // This is because, any of the nodes may have its rate limit related info, specified
        // via properties. Such a node needs to be accepted at this point as property
        // sourced rate limited data will later be transferred to class/method nodes
        return RateProcessors.ofClass(source -> true);
    }

    private RateProcessor<RateLimitProperties> getPropertyRateProcessor() {
        return RateProcessors.ofProperties();
    }

    private static final class RateConfigCollector implements RateProcessor.NodeConsumer {
        private final Map<String, RateConfig> nameToRateMap;
        public RateConfigCollector() {
            this.nameToRateMap = new HashMap<>();
        }
        @Override
        public void accept(Object genericDeclaration, Node<RateConfig> node) {
            node.getValueOptional().ifPresent(
                    rateConfig -> nameToRateMap.putIfAbsent(node.getName(), rateConfig));
        }
        public RateConfig getOrNull(String name) {
            return nameToRateMap.get(name);
        }
    }
}
