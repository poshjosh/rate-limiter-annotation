package io.github.poshjosh.ratelimiter.matcher;

import io.github.poshjosh.ratelimiter.annotation.exceptions.NodeValueAbsentException;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HasParentLimitTest {
    
    private final HasParentLimit hasParentLimit = new HasParentLimit();

    @Test
    void givenNodeValueIsNull_throwsException() {
        Node<RateConfig> node = Nodes.of("node-has-no-value", null, null);
        assertThrows(NodeValueAbsentException.class, () -> hasParentLimit.test(node));
    }

    @Test
    void givenNodeHasLimit_returnsTrue() {
        assertTrue(hasParentLimit.test(Scenarios.givenNodeHasLimit()));
    }

    @Test
    void givenNodeHasNoLimit_returnsFalse() {
        assertFalse(hasParentLimit.test(Scenarios.givenNodeHasNoLimit()));
    }

    @Test
    void givenNodeParentHasLimit_returnsTrue() {
        assertTrue(hasParentLimit.test(Scenarios.givenNodeParentHasLimit()));
    }

    @Test
    void givenNodeChildHasLimit_returnsFalse() {
        assertFalse(hasParentLimit.test(Scenarios.givenNodeChildHasLimit()));
    }

    @Test
    void givenNodeValueParentHasLimit_returnsFalse() {
        assertFalse(hasParentLimit.test(Scenarios.givenNodeValueParentHasLimit()));
    }

    @Test
    void givenNodeValueChildHasLimit_returnsFalse() {
        assertFalse(hasParentLimit.test(Scenarios.givenNodeValueChildHasLimit()));
    }
}
