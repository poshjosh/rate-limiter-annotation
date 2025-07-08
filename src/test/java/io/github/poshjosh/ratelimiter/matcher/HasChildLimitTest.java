package io.github.poshjosh.ratelimiter.matcher;

import io.github.poshjosh.ratelimiter.annotation.exceptions.NodeValueAbsentException;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.Nodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HasChildLimitTest {
    
    private final HasChildLimit hasChildLimit = new HasChildLimit();

    @Test
    void givenNodeValueIsNull_throwsException() {
        Node<RateConfig> node = Nodes.of("node-has-no-value", null, null);
        assertThrows(NodeValueAbsentException.class, () -> hasChildLimit.test(node));
    }

    @Test
    void givenNodeHasLimit_returnsTrue() {
        assertTrue(hasChildLimit.test(Scenarios.givenNodeHasLimit()));
    }

    @Test
    void givenNodeHasNoLimit_returnsFalse() {
        assertFalse(hasChildLimit.test(Scenarios.givenNodeHasNoLimit()));
    }

    @Test
    void givenNodeParentHasLimit_returnsFalse() {
        assertFalse(hasChildLimit.test(Scenarios.givenNodeParentHasLimit()));
    }

    @Test
    void givenNodeChildHasLimit_returnsTrue() {
        assertTrue(hasChildLimit.test(Scenarios.givenNodeChildHasLimit()));
    }

    @Test
    void givenNodeValueParentHasLimit_returnsFalse() {
        assertFalse(hasChildLimit.test(Scenarios.givenNodeValueParentHasLimit()));
    }

    @Test
    void givenNodeValueChildHasLimit_returnsFalse() {
        assertFalse(hasChildLimit.test(Scenarios.givenNodeValueChildHasLimit()));
    }
}
