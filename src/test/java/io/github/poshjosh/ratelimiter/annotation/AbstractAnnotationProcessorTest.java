package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.model.RateConfig;

import java.lang.reflect.GenericDeclaration;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Fail.fail;

public abstract class AbstractAnnotationProcessorTest<S extends GenericDeclaration> {

    void assertHasChildHavingNames(Node<RateConfig> parent, S... classes) {
        assertHasChildHavingNames(parent, toNames(classes));
    }

    void assertHasChildHavingNames(Node<RateConfig> parent, String... names) {
        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final Node<RateConfig> child = parent.getChild(i);
            if (acceptNodeNames(child, names)) {
                return;
            }
        }
        fail("Expected parent node to have a child with one of the names: " + Arrays.toString(names) +
                ", but found no such child in: " + parent);
    }

    String [] toNames(S... classes) {
        return Arrays.stream(classes).map(clazz -> getId(clazz)).collect(Collectors.toList()).toArray(new String[0]);
    }

    boolean acceptNodeNames(Node<RateConfig> node, String... names) {
        for(String name : names) {
            if(name.equals(node.getName())) {
                return true;
            }
        }
        return false;
    }

    abstract String getId(S element);
}
