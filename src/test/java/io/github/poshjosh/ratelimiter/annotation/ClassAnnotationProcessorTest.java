package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.NodeFormatter;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ClassAnnotationProcessorTest extends AbstractAnnotationProcessorTest<Class<?>> {

    @Test
    public void methodNodesFromSuperClassesShouldBeTransferredToResourceAnnotatedClass() {
        // @TODO
    }

    @Test
    public void classCannotHaveMultipleClassLevelResourceAnnotationsInHeirarchy() {
        // @TODO
    }

    @Test
    public void nodeVisitingShouldBeAccurate() {
        List<Class<?>> classes = findClasses();
//        System.out.println("Found classes: " + classes);
        final String rootNodeName = "sample-root-node";
        Node<RateConfig> root = Node.of(rootNodeName);
        getInstance().processAll(root, classes);
        System.out.println();
        System.out.println(NodeFormatter.indentedHeirarchy().format(root));
        assertThat(root.findFirstChild(node -> node.getName().equals(rootNodeName)).isPresent()).isTrue();
        assertHasChildrenHavingNames(root, "ClassGroupOnlyAnon", "PrivateClass", "InternalClass");
        assertHasChildrenHavingNames(root, "Fire");
        Node<RateConfig> fire = root.findFirstChild(node -> "Fire".equals(node.getName())).orElse(null);
        assertHasChildrenHavingNames(fire,
                ClassWithClassAnnotations.ClassGroupOnlyNamedFire.class,
                ClassWithClassAnnotations.SecondClassGroupOnlyNamedFire.class);
    }

    AnnotationProcessor<Class<?>> getInstance() {
        return new ClassAnnotationProcessor() {
            @Override protected Element toElement(Class<?> element) {
                return new Element() {
                    @Override public Element getDeclarer() {
                        return this;
                    }
                    @Override public String getId() {
                        return ClassAnnotationProcessorTest.this.getId(element);
                    }
                    @Override public <T extends Annotation> Optional<T> getAnnotation(
                            Class<T> annotationClass) {
                        return Optional.ofNullable(element.getAnnotation(annotationClass));
                    }
                };
            }
        };
    }

    String getId(Class<?> element) {
        return element.getSimpleName();
    }
}