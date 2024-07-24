package io.github.poshjosh.ratelimiter.node;

public class TestNode {

    public static Node<Integer> breadthFirst() {
        return breadthFirst(0, 1, 2, 3, 4);
    }

    public static Node<Integer> breadthFirst(Integer... numbers) {
        int i = 0;
        Node<Integer> parent = Nodes.of("parent", numbers[i++]);
        Node<Integer> son = Nodes.of("son", numbers[i++], parent);
        Node<Integer> daughter = Nodes.of("daughter", numbers[i++], parent);
        Node<Integer> grandSon = Nodes.of("grand-son", numbers[i++], son);
        Node<Integer> grandDaughter = Nodes.of("grand-daughter", numbers[i++], son);
        return parent;
    }

    public static Node<Integer> depthFirst() {
        return breadthFirst(0, 1, 2, 3, 4);
    }

    public static Node<Integer> depthFirst(Integer... numbers) {
        int i = 0;
        Node<Integer> parent = Nodes.of("parent", numbers[i++]);
        Node<Integer> son = Nodes.of("son", numbers[i++], parent);
        Node<Integer> grandSon = Nodes.of("grand-son", numbers[i++], son);
        Node<Integer> grandDaughter = Nodes.of("grand-daughter", numbers[i++], son);
        Node<Integer> daughter = Nodes.of("daughter", numbers[i++], parent);
        return parent;
    }
}
