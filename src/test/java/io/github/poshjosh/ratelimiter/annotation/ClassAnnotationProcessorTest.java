package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.util.Rates;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
        final List<Class<?>> classes = Arrays.asList(
                ClassWithClassAnnotations.class,
                ClassWithClassAnnotations.ClassGroupOnlyAnon.class,
                ClassWithClassAnnotations.ClassGroupOnlyNamedFire.class,
                ClassWithClassAnnotations.ClassWithInternalClass.class,
                ClassWithClassAnnotations.ClassWithInternalClass.InternalClass.class,
                ClassWithClassAnnotations.PrivateClass.class,
                ClassWithClassAnnotations.GroupAnnotationOnly.class,
                ClassWithClassAnnotations.SecondClassGroupOnlyNamedFire.class,
                ClassWithMethodAnnotations.class,
                ClassWithMethodAnnotations.MethodGroupOnlyAnon.class

        );
        final String rootNodeName = "sample-root-node";
        Node<RateConfig> root = Node.of(rootNodeName);
        root = getInstance().processAll(root, classes);
        System.out.println(root);
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

    public static class ClassWithClassAnnotations {

        @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @Rate(permits = 10, timeUnit = MILLISECONDS)
        public class ClassGroupOnlyAnon { }

        @RateGroup(name = "Fire", operator = Operator.AND)
        @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @Rate(permits = 10, timeUnit = MILLISECONDS)
        @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @Rate(permits = 10, timeUnit = MILLISECONDS)
        @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @Rate(permits = 10, timeUnit = MILLISECONDS)
        public class ClassGroupOnlyNamedFire { }

        public class ClassWithInternalClass {
            @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
            @Rate(permits = 10, timeUnit = MILLISECONDS)
            public class InternalClass{ }
        }

        @RateGroup("x")
        public class GroupAnnotationOnly { }

        @RateGroup("Fire")
        public class SecondClassGroupOnlyNamedFire { }

        @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @Rate(permits = 10, timeUnit = MILLISECONDS)
        class PrivateClass{ }
    }

    public static class ClassWithMethodAnnotations {
        public class MethodGroupOnlyAnon {
            @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
            @Rate(permits = 10, timeUnit = MILLISECONDS)
            void anon() { }
        }

        @RateGroup("Fire")
        void fire() { }
    }
}