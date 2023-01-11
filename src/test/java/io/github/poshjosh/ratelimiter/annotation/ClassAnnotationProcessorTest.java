package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.node.NodeFormatter;
import io.github.poshjosh.ratelimiter.util.Rates;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ClassAnnotationProcessorTest extends AbstractAnnotationProcessorTest<Class<?>> {

    @Test
    void methodNodesFromSuperClassesShouldBeTransferredToResourceAnnotatedClass() {
        // @TODO
    }

    @Test
    void classCannotHaveMultipleClassLevelResourceAnnotationsInHeirarchy() {
        // @TODO
    }

    @Test
    void nodeVisitingShouldBeAccurate() {
        List<Class<?>> classes = findClasses();
//        System.out.println("Found classes: " + classes);
        final String rootNodeName = "sample-root-node";
        Node<RateConfig> root = Node.of(rootNodeName);
        getInstance().processAll(root, classes);
        System.out.println();
        System.out.println(NodeFormatter.indentedHeirarchy("\t").format(root));
        assertThat(root.findFirstChild(node -> node.getName().equals(rootNodeName)).isPresent()).isTrue();
        assertHasChildrenHavingNames(root, "ClassGroupOnlyAnon", "PrivateClass", "InternalClass");
        assertHasChildrenHavingNames(root, "Fire");
        Node<RateConfig> fire = root.findFirstChild(node -> "Fire".equals(node.getName())).orElse(null);
        assertHasChildrenHavingNames(fire,
                ClassWithClassAnnotations.ClassGroupOnlyNamedFire.class,
                ClassWithClassAnnotations.SecondClassGroupOnlyNamedFire.class);
    }

    AnnotationProcessor<Class<?>> getInstance() {
        AnnotationConverter<Rate, Rates> converter = AnnotationConverter.ofRate();
        AnnotationProcessor<Method> methodProcessor = new MethodAnnotationProcessor(converter) {
            @Override protected Element toElement(Method element) {
                return ClassAnnotationProcessorTest.this.toElement(element);
            }
        };
        return new ClassAnnotationProcessor(methodProcessor, converter) {
            @Override protected Element toElement(Class<?> element) {
                return ClassAnnotationProcessorTest.this.toElement(element);
            }
        };
    }

    private Element toElement(Method element) {
        return new Element() {
            @Override public Element getDeclarer() {
                return ClassAnnotationProcessorTest.this.toElement(element.getDeclaringClass());
            }
            @Override public String getId() {
                return getDeclarer().getId() + '#' + element.getName();
            }
            @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annClass) {
                return Optional.ofNullable(element.getAnnotation(annClass));
            }
        };
    }

    private Element toElement(Class<?> element) {
        return new Element() {
            @Override public Element getDeclarer() {
                return this;
            }
            @Override public String getId() {
                return ClassAnnotationProcessorTest.this.getId(element);
            }
            @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annClass) {
                return Optional.ofNullable(element.getAnnotation(annClass));
            }
        };
    }

    String getId(Class<?> element) {
        return element.getSimpleName();
    }
}