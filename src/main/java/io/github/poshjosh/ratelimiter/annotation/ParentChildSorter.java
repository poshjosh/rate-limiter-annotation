package io.github.poshjosh.ratelimiter.annotation;

import java.util.*;
import java.util.function.UnaryOperator;

final class ParentChildSorter {
    /**
     * Sort a list of elements such that parents come before children.
     * This method is not optimized for performance, but rather for simplicity.
     * @param list The list to sort with parents before children.
     * @param getParent A function that returns the parent of an element.
     * @return A new list with the elements sorted such that parents come before children.
     * @param <T> The type of the elements in the list.
     */
    static <T> List<T> sortParentBeforeChild(List<T> list, UnaryOperator<T> getParent) {
        LinkedHashSet<T> sorted = new LinkedHashSet<>(list.size() * 2);
        for(T e : list) {
            recursivelyAddParentBeforeChild(sorted, e, getParent);
        }
        return new ArrayList<>(sorted);
    }

    /**
     * Sort a list of elements such that children come before parents.
     * This method is not optimized for performance, but rather for simplicity.
     * @param list The list to sort with children before parents.
     * @param getParent A function that returns the parent of an element.
     * @return A new list with the elements sorted such that children come before parents.
     * @param <T> The type of the elements in the list.
     */
    static <T> List<T> sortChildBeforeParent(List<T> list, UnaryOperator<T> getParent) {
        List<T> sorted = new ArrayList<>(list.size());
        for(T e : list) {
            recursivelyAddChildBeforeParent(sorted, e, getParent);
        }
        return sorted;
    }

    private static <T> void recursivelyAddParentBeforeChild(
            LinkedHashSet<T> addTo, T e, UnaryOperator<T> getParent) {
        T parent = getParent.apply(e);
        if (parent != null) {
            recursivelyAddParentBeforeChild(addTo, parent, getParent);
        }
        addTo.add(e);
    }

    private static <T> void recursivelyAddChildBeforeParent(
            List<T> addTo, T e, UnaryOperator<T> getParent) {
        T parent = getParent.apply(e);
        if (parent != null) {
            recursivelyAddChildBeforeParent(addTo, parent, getParent);
        }
        if (!addTo.contains(e)) {
            addTo.add(0, e);
        }
    }

    private ParentChildSorter() { }
}
