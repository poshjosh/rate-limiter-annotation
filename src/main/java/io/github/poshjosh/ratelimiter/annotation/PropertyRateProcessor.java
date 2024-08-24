package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotation.exceptions.DuplicateNameException;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;

import java.util.*;
import java.util.function.UnaryOperator;

final class PropertyRateProcessor implements RateProcessor<RateLimitProperties> {
    PropertyRateProcessor() { }

    @Override
    public Node<RateConfig> process(
            Node<RateConfig> root, NodeConsumer consumer, RateLimitProperties source) {
        return addNodesToRoot(root, source, consumer);
    }

    private Node<RateConfig> addNodesToRoot(Node<RateConfig> rootNode, RateLimitProperties source,
            NodeConsumer nodeConsumer) {
        List<Rates> ratesList = source.getRates();
        checkNamesNotConflicting(rootNode, ratesList);
        createNodes(rootNode, source, nodeConsumer);
        return rootNode;
    }

    private void checkNamesNotConflicting(Node<RateConfig> rootNode, List<Rates> ratesList) {
        final String rootName = rootNode.getName();
        if (ratesList.stream().map(Rates::getId).anyMatch(rootName::equals)) {
            throw new IllegalStateException("The name: " + rootNode.getName()
                    + " is reserved, and may not be used to identify rates in "
                    + RateLimitProperties.class.getName());
        }
        if (new HashSet<>(ratesList).size() != ratesList.size()) {
            throw new DuplicateNameException("Duplicate rate ids found in "
                    + RateLimitProperties.class.getName());
        }
    }

    private void createNodes(
            Node<RateConfig> root,
            RateLimitProperties source,
            NodeConsumer nodeConsumer) {
        final List<Rates> ratesList = sortParentBeforeChild(source.getRates());
        final Map<String, Node<RateConfig>> processed = new HashMap<>();
        for (Rates rates : ratesList) {
            final String id = rates.getId();
            final Node<RateConfig> parent = rates.getParentId() == null
                    ? root : processed.getOrDefault(rates.getParentId(), root);
            final RateSource rateSource = PropertyRateSource.of(source, rates);
            final RateConfig parentConfig = parent.getValueOrDefault(null);
            final Node<RateConfig> node = Nodes
                    .of(id, RateConfig.of(rateSource, rates, parentConfig), parent);
            nodeConsumer.accept(rates, node);
            processed.put(id, node);
        }
    }

    private List<Rates> sortParentBeforeChild(List<Rates> ratesList) {
        if (ratesList.isEmpty() || ratesList.size() == 1) {
            return ratesList;
        }
        final Map<String, Rates> ratesMap = new HashMap<>();
        ratesList.forEach(rates -> ratesMap.put(rates.getId(), rates));
        final UnaryOperator<Rates> getParent = rates -> {
            final String parentId = rates.getParentId();
            return parentId == null ? null : ratesMap.get(parentId);
        };
        return ParentChildSorter.sortParentBeforeChild(ratesList, getParent);
    }
}
