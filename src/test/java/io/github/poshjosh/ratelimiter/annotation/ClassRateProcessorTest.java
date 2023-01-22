package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.Operator;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.util.Rates;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ClassRateProcessorTest extends AbstractAnnotationProcessorTest<Class<?>> {

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
    final Class<?>[] classes =
        new Class[] {
                  GroupAnnotationsOnlyGroup.class,
                  ClassWithClassAnnotationsGroup.class,
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
        };
        Node<RateConfig> root = getInstance().processAll(classes);
        System.out.println(root);
        assertThat(root.findFirstChild(node -> node.getName().equals(root.getName())).isPresent()).isTrue();
        assertHasChildrenHavingNames(root, "ClassGroupOnlyAnon", "PrivateClass", "InternalClass");
        assertHasChildrenHavingNames(root, "Fire");
        Node<RateConfig> fire = root.findFirstChild(node -> "Fire".equals(node.getName()))
                .orElseThrow(NullPointerException::new);
        assertHasChildrenHavingNames(fire,
                ClassWithClassAnnotations.ClassGroupOnlyNamedFire.class,
                ClassWithClassAnnotations.SecondClassGroupOnlyNamedFire.class);
    }

    RateProcessor<Class<?>> getInstance() {
        RateProcessor.SourceFilter sourceTest = RateProcessor.SourceFilter.ofRateLimited();
        AnnotationConverter<Rate, Rates> converter = AnnotationConverter.ofRate();
        RateProcessor<Method> methodProcessor = new MethodRateAnnotationProcessor(sourceTest, converter) {
            @Override protected Element toElement(Method element) {
                return ClassRateProcessorTest.this.toElement(element);
            }
        };
        return new ClassRateAnnotationProcessor(sourceTest, converter, methodProcessor) {
            @Override protected Element toElement(Class<?> element) {
                return ClassRateProcessorTest.this.toElement(element);
            }
        };
    }

    private Element toElement(Method element) {
        return new Element() {
            @Override public boolean isRateLimited() {
                return RateProcessor.SourceFilter.ofRateLimited().test(element);
            }
            @Override public Element getDeclarer() {
                return ClassRateProcessorTest.this.toElement(element.getDeclaringClass());
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
            @Override public boolean isRateLimited() {
                return RateProcessor.SourceFilter.ofRateLimited().test(element);
            }
            @Override public Element getDeclarer() {
                return this;
            }
            @Override public String getId() {
                return ClassRateProcessorTest.this.getId(element);
            }
            @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annClass) {
                return Optional.ofNullable(element.getAnnotation(annClass));
            }
        };
    }

    String getId(Class<?> element) {
        return element.getSimpleName();
    }

    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    @RateGroup(name = "Fire", operator = Operator.AND)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    public @interface ClassWithClassAnnotationsGroup { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @RateGroup("x")
    public @interface GroupAnnotationsOnlyGroup { }

    public static class ClassWithClassAnnotations {

        @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @Rate(permits = 10, timeUnit = MILLISECONDS)
        public class ClassGroupOnlyAnon { }

        @ClassWithClassAnnotationsGroup
        public class ClassGroupOnlyNamedFire { }

        public class ClassWithInternalClass {
            @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
            @Rate(permits = 10, timeUnit = MILLISECONDS)
            public class InternalClass{ }
        }


        @GroupAnnotationsOnlyGroup
        public class GroupAnnotationOnly { }

        @ClassWithClassAnnotationsGroup
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

        @ClassWithClassAnnotationsGroup
        void fire() { }
    }
}