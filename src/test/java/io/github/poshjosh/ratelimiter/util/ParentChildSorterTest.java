package io.github.poshjosh.ratelimiter.util;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParentChildSorterTest {

    @Test
    void testSortParentBeforeChild() {

        final List<Element> unsorted = givenUnsortedList();

        final UnaryOperator<Element> getParent = new ElementParentAccessor(unsorted);

        final List<Element> sorted = ParentChildSorter.sortParentBeforeChild(unsorted, getParent);

        assertThatElementIndicesAreValid(sorted, getParent, true);
    }


    @Test
    void testSortChildBeforeParent() {

        final List<Element> unsorted = givenUnsortedList();

        final UnaryOperator<Element> getParent = new ElementParentAccessor(unsorted);

        final List<Element> sorted = ParentChildSorter.sortChildBeforeParent(unsorted, getParent);

        assertThatElementIndicesAreValid(sorted, getParent, false);
    }

    private static void assertThatElementIndicesAreValid(
            List<Element> sorted, UnaryOperator<Element> getParent, boolean parentBeforeChild) {
        for (int i = 0; i < sorted.size(); i++) {
            final Element element = sorted.get(i);
            final Element parent = getParent.apply(element);
            if (parent == null) {
                continue;
            }
            final int indexOfParent = sorted.indexOf(parent);
            if (parentBeforeChild) {
                assertTrue(indexOfParent < i,
                        "Expected parent to be before child. Found parent at index " +
                                indexOfParent + ": " + parent + ", child at index " + i + ": " + element);
            } else {
                assertTrue(indexOfParent > i,
                        "Expected child to be before parent. Found child at index " +
                        i + ": " + element + ", parent at index " + indexOfParent + ": " + parent);
            }
        }
    }

    private List<Element> givenUnsortedList() {
        Element e4 = new Element("4", "2");
        Element e3 = new Element("3", "1");
        Element e1 = new Element("1", "2");
        Element e2 = new Element("2", null);

        List<Element> list = new ArrayList<>(Arrays.asList(e1, e2, e3, e4));
        int index = list.size() + 1;
        for (int i = 0; i < 1000; i++) {
            list.add(new Element(String.valueOf(index + i), "4"));
        }
        index = list.size() + 1;
        for (int i = 0; i < 1000; i++) {
            list.add(new Element(String.valueOf(index + i), "3"));
        }

        Collections.shuffle(list);
        return list;
    }

    private static class ElementParentAccessor implements UnaryOperator<Element> {
        private final Map<String, Element> elements;
        private ElementParentAccessor(List<Element> elements) {
            this.elements = new HashMap<>();
            elements.forEach(e -> this.elements.put(e.id, e));
        }
        public Element apply(Element element) {
            return element.parentId == null ? null : elements.get(element.parentId);
        }
    }

    private static class Element {
        private final String id;
        private final String parentId;
        public Element(String id, String parentId) {
            this.id = id;
            this.parentId = parentId;
        }

        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            Element element = (Element) o;

            if (!id.equals(element.id))
                return false;
            return Objects.equals(parentId, element.parentId);
        }

        @Override public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
            return result;
        }

        @Override public String toString() {
            return "Element{" + "id='" + id + '\'' + ", parentId='" + parentId + '\'' + '}';
        }
    }
}
