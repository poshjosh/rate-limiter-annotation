package io.github.poshjosh.ratelimiter.node;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class DepthFirstNodeVisitorTest {

    @Test
    void shouldVisitEachNodeOnce() {
        Node<Integer> node = TestNode.breadthFirst();
        int expected = node.size();
        AtomicInteger sum = new AtomicInteger();
        Consumer<Node<Integer>> consumer = e -> sum.incrementAndGet();
        visitor(consumer).accept(node);
        assertEquals(expected, sum.get());
    }

    @Test
    void shouldVisitDepthFirst() {
        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5);
        Node<Integer> node = TestNode.depthFirst(values.toArray(new Integer[0]));
        List<Integer> collected = new ArrayList<>(values.size());
        Consumer<Node<Integer>> consumer = e -> collected.add(e.getValueOrDefault(0));
        visitor(consumer).accept(node);
        assertEquals(values, collected);
    }

    Consumer<Node<Integer>> visitor(Consumer<Node<Integer>> consumer) {
        return new DepthFirstNodeVisitor<>(consumer);
    }
}