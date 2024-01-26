package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
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
        Map<String, Rates> configsWithoutParent = new LinkedHashMap<>(limits);
        Rates rootNodeConfig = configsWithoutParent.remove(rootNode.getName());
        if (rootNodeConfig != null) {
            throw new IllegalStateException("The name: " + rootNode.getName()
                    + " is reserved, and may not be used to identify rates in "
                    + RateLimitProperties.class.getName());
        }
        createNodes(rootNode, nodeConsumer, source, configsWithoutParent);
        return rootNode;
    }

    private void createNodes(Node<RateConfig> parent, NodeConsumer nodeConsumer,
            RateLimitProperties source, Map<String, Rates> limits) {
        Set<Map.Entry<String, Rates>> entrySet = limits.entrySet();
        for (Map.Entry<String, Rates> entry : entrySet) {
            String name = entry.getKey();
            requireParentNameDoesNotMatchChild(parent.getName(), name);
            Rates rates = entry.getValue();
            RateSource rateSource = new PropertyRateSource(name, rates.hasLimitsSet(), source);
            RateConfig parentConfig = parent.getValueOrDefault(null);
            Node<RateConfig> node = Node.of(name, RateConfig.of(rateSource, rates, parentConfig), parent);
            nodeConsumer.accept(rates, node);
        }
    }

    private void requireParentNameDoesNotMatchChild(String parent, String child) {
        if (Objects.equals(parent, child)) {
            throw new IllegalStateException(
                    "Parent and child nodes may not have the same name: " + parent);
        }
    }
}
