package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class PropertyRateProcessor implements RateProcessor<RateLimitProperties> {
    PropertyRateProcessor() { }

    @Override
    public Node<RateConfig> process(Node<RateConfig> root, NodeConsumer consumer,
            RateLimitProperties source) {
        return addNodesToRoot(root, source, consumer);
    }

    private Node<RateConfig> addNodesToRoot(Node<RateConfig> rootNode, RateLimitProperties source,
            NodeConsumer nodeConsumer) {
        Map<String, Rates> limits = source.getRateLimitConfigs();
        Rates rootNodeConfig = limits.get(rootNode.getName());
        if (rootNodeConfig != null) {
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
        final Set<Map.Entry<String, Rates>> entrySet = source.getRateLimitConfigs().entrySet();
        for (Map.Entry<String, Rates> entry : entrySet) {
            final String name = entry.getKey();
            if (parent.getName().equals(name)) {
                continue;
            }
            final Rates rates = entry.getValue();
            final RateSource rateSource = PropertyRateSource.of(source, name);
            final RateConfig parentConfig = parent.getValueOrDefault(null);
            final Node<RateConfig> node = Nodes
                    .of(name, RateConfig.of(rateSource, rates, parentConfig), parent);
            nodeConsumer.accept(rates, node);
        }
    }
}
