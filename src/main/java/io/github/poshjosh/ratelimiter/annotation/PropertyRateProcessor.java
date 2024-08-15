package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;

import java.util.*;

final class PropertyRateProcessor implements RateProcessor<RateLimitProperties> {
    PropertyRateProcessor() { }

    @Override
    public Node<RateConfig> process(Node<RateConfig> root, NodeConsumer consumer,
            RateLimitProperties source) {
        return addNodesToRoot(root, source, consumer);
    }

    private Node<RateConfig> addNodesToRoot(Node<RateConfig> rootNode, RateLimitProperties source,
            NodeConsumer nodeConsumer) {
        List<Rates> ratesList = source.getRateLimitConfigs();
        if (ratesList.stream().anyMatch(rates -> rootNode.getName().equals(rates.getId()))) {
            throw new IllegalStateException("The name: " + rootNode.getName()
                    + " is reserved, and may not be used to identify rates in "
                    + RateLimitProperties.class.getName());
        }
        createNodes(rootNode, source, nodeConsumer);
        return rootNode;
    }

    private void createNodes(
            Node<RateConfig> parent,
            RateLimitProperties source,
            NodeConsumer nodeConsumer) {
        final List<Rates> ratesList = source.getRateLimitConfigs();
        for (Rates rates : ratesList) {
            final String name = rates.getId();
            if (parent.getName().equals(name)) {
                continue;
            }
            final RateSource rateSource = PropertyRateSource.of(source, rates);
            final RateConfig parentConfig = parent.getValueOrDefault(null);
            final Node<RateConfig> node = Nodes
                    .of(name, RateConfig.of(rateSource, rates, parentConfig), parent);
            nodeConsumer.accept(rates, node);
        }
    }
}
