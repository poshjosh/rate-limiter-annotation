package io.github.poshjosh.ratelimiter.node;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void testIsRoot() {
        Node<Integer> node = TestNode.breadthFirst();
        assertTrue(node.getRoot().isRoot());
        assertFalse(node.getRoot().getChild(0).isRoot());
    }

    @Test
    void getChildren() {
        Node<Integer> node = TestNode.breadthFirst();
        List<Node<Integer>> expected = node.getChildren();
        assertEquals(2, expected.size());
        List<Node<Integer>> found = new ArrayList<>();
        node.visitAll(e -> Objects.equals(node, e.getParentOrDefault(null)), found::add);
        assertEquals(expected, found);
    }

    @Test
    void findFirstChild_givenValidTest_shouldHitOneResult() {
        assertTrue(TestNode.breadthFirst().findFirstChild(node -> "son".equals(node.getName())).isPresent());
    }

    @Test
    void transform_givenValuesConverter_shouldTransformValues() {
        Node<Integer> update = TestNode.breadthFirst().transform(node -> 0);
        assertFalse(update.findFirstChild(node -> node.getValueOrDefault(0) != 0).isPresent());
    }

    @Test
    void transform_givenCallingNodeAsParent_shouldThrowStackOverflowError() {
        Node<Integer> root = TestNode.breadthFirst();
        assertThrows(StackOverflowError.class, () -> root.transform(root, node -> 0));
    }

    @Test
    void transform_canCopy() {
        Node<Integer> expected = TestNode.breadthFirst();
        Node<Integer> found = expected.transform(node -> node.getValueOrDefault(0));
        assertEquals(expected, found);
    }

    @Test
    void transform_canDelete() {
        Node<Integer> expected = TestNode.breadthFirst();
        Predicate<Node<Integer>> test = node -> {
            Integer value = node.getValueOrDefault(0);
            return value == 0 || value == 2;
        };
        Node<Integer> found = expected.transform(test, node -> node.getValueOrDefault(0))
                .orElseThrow(() -> new RuntimeException("Did I mean to delete all from the tree?"));
        assertTrue(found.findFirstChild(test).isPresent());
        assertFalse(found.findFirstChild(test.negate()).isPresent());
    }
}