package io.github.poshjosh.ratelimiter.node;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class DepthFirstVisitorTest {

    @Test
    void shouldVisitEachNodeOnce() {
        Node<Integer> node = TestNode.breadthFirst();
        int expected = node.size();
        AtomicInteger sum = new AtomicInteger();
        Consumer<Node<Integer>> consumer = e -> sum.incrementAndGet();
        DepthFirstVisitor.visitAll(node, consumer);
        assertEquals(expected, sum.get());
    }

    @Test
    void shouldVisitDepthFirst() {
        final Integer [] values = TestNode.values();
        Node<Integer> node = TestNode.depthFirst();
        List<Integer> collected = new ArrayList<>(values.length);
        Consumer<Node<Integer>> consumer = e -> collected.add(e.getValueOrDefault(0));
        DepthFirstVisitor.visitAll(node, consumer);
        assertEquals(Arrays.asList(values), collected);
    }
}