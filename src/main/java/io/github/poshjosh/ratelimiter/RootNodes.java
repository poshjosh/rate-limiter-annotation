package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.annotation.RateProcessors;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
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

    private final Node<RateContext<K>> propertiesRootNode;
    private final Node<RateContext<K>>[] propertiesLeafNodes;
    private final boolean hasProperties;

    private final Node<RateContext<K>> annotationsRootNode;
    private final Node<RateContext<K>>[] annotationsLeafNodes;
    private final boolean hasAnnotations;

    private RootNodes(RateLimiterContext<K> context) {

        RateConfigCollector propertyConfigs = new RateConfigCollector();
        Node<RateConfig> propRoot = getPropertyRateProcessor()
                .process(Nodes.of("root.properties"), propertyConfigs, context.getProperties());

        Node<RateConfig> annoRoot = getClassRateProcessor()
                .processAll(Nodes.of("root.annotations"),
                        (src, node) -> {}, context.getTargetClasses());

        List<String> transferredToAnnotations = new ArrayList<>();
        Function<Node<RateConfig>, RateConfig> overrideWithPropertyValue = node -> {
            if (node.isRoot()) {
                return node.getValueOrDefault(null);
            }
            RateConfig annotationConfig = node.requireValue();
            return propertyConfigs.get(node.getName())
                    .map(propertyConfig -> {
                        transferredToAnnotations.add(node.getName());
                        return propertyConfig.withSource(annotationConfig.getSource());
                    }).orElse(annotationConfig);
        };

        annoRoot = annoRoot.transform(overrideWithPropertyValue);

        Predicate<Node<RateConfig>> isNodeRateLimited = node -> {
            if (node.isRoot()) {
                return true;
            }
            return transferredToAnnotations.contains(node.getName()) || node.getValueOptional()
                    .map(RateConfig::getSource)
                    .filter(RateSource::isRateLimited).isPresent();
        };

        Predicate<Node<RateConfig>> anyNodeInTreeIsRateLimited =
                node -> node.anyMatch(isNodeRateLimited);

        Function<Node<RateConfig>, RateContext<K>> transformer = currentNode ->
                RateContext.of(context.getMatcherProvider(), currentNode);

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

        hasProperties = !propertiesRootNode.isEmptyNode() && propertiesRootNode.size() > 0;
        hasAnnotations = !annotationsRootNode.isEmptyNode() && annotationsRootNode.size() > 0;

        if (RateContext.IS_BOTTOM_UP_TRAVERSAL) {
            propertiesLeafNodes = collectLeafs(propertiesRootNode);
            annotationsLeafNodes = collectLeafs(annotationsRootNode);
        } else {
            propertiesLeafNodes = null;
            annotationsLeafNodes = null;
        }
    }
    private <R> Node<RateContext<R>> [] collectLeafs(Node<RateContext<R>> node) {
        Set<Node<RateContext<R>>> leafNodes = new LinkedHashSet<>();
        Predicate<Node<RateContext<R>>> test = n -> n.isLeaf() && n.hasValue();
        node.getRoot().visitAll(test, leafNodes::add);
        return leafNodes.toArray(new Node[0]);
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

    public boolean hasProperties() {
        return hasProperties;
    }

    public boolean hasAnnotations() {
        return hasAnnotations;
    }

    public Node<RateContext<K>> getPropertiesRootNode() {
        return propertiesRootNode;
    }

    public Node<RateContext<K>> getAnnotationsRootNode() {
        return annotationsRootNode;
    }

    public Node<RateContext<K>>[] getPropertiesLeafNodes() {
        return propertiesLeafNodes;
    }

    public Node<RateContext<K>>[] getAnnotationsLeafNodes() {
        return annotationsLeafNodes;
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
        public Optional<RateConfig> get(String name) {
            return Optional.ofNullable(nameToRateMap.get(name));
        }
    }
}
