package io.github.poshjosh.ratelimiter.matcher;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.model.RateSources;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;

class Scenarios {
    static Node<RateConfig> givenNodeHasLimit() {
        return Nodes.of("node-has-limit", RateConfig.of(1));
    }

    static Node<RateConfig> givenNodeHasNoLimit() {
        return Nodes.of("node-has-no-limit", RateConfig.of(Rates.empty()));
    }

    static Node<RateConfig> givenNodeParentHasLimit() {
        Node<RateConfig> parent = Nodes.of("parent", RateConfig.of(1));
        return Nodes.of("node-parent-has-limit", RateConfig.NONE, parent);
    }

    static Node<RateConfig> givenNodeChildHasLimit() {
        Node<RateConfig> parent = Nodes.of("node-child-has-limit", RateConfig.NONE);
        Nodes.of("child", RateConfig.of(1), parent);
        return parent;
    }

    static Node<RateConfig> givenNodeValueParentHasLimit() {
        return Nodes.of("node-value-parent-has-limit", givenRateConfigParentHasLimit());
    }

    static Node<RateConfig> givenNodeValueChildHasLimit() {
        return Nodes.of("node-value-child-has-limit", givenRateConfigChildHasLimit());
    }

    private static RateConfig givenRateConfigParentHasLimit() {
        return RateConfig.of(RateSources.NONE, Rates.empty(), RateConfig.of(1));
    }

    private static RateConfig givenRateConfigChildHasLimit() {
        RateConfig parentConfig = RateConfig.of(Rates.empty());
        RateConfig.of(RateSources.NONE, Rates.of(Rate.ofSeconds(1)), parentConfig);
        return parentConfig;
    }
}
