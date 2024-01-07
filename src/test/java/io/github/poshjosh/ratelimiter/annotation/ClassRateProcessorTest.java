package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.model.RateConfig;
import org.junit.jupiter.api.Test;
import java.lang.annotation.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ClassRateProcessorTest extends AbstractAnnotationProcessorTest<Class<?>> {

    @Test
    void methodNodesFromSuperClassesShouldBeTransferredToResourceAnnotatedClass() {
        // @TODO
    }

    @Test
    void classCannotHaveMultipleClassLevelResourceAnnotationsInHierarchy() {
        // @TODO
    }

    @Test
    void nodeVisitingShouldBeAccurate() {
        final Class<?>[] classes = new Class[] {
                          GroupAnnotationsOnlyGroup.class,
                          GroupAnnotationWithoutName.class,
                          ClassWithClassAnnotations.class,
                          ClassWithClassAnnotations.ClassGroupOnlyAnon.class,
                          ClassWithClassAnnotations.ClassGroupOnly_GroupAnnotationWithoutName.class,
                          ClassWithClassAnnotations.ClassWithInternalClass.class,
                          ClassWithClassAnnotations.ClassWithInternalClass.InternalClass.class,
                          ClassWithClassAnnotations.PrivateClass.class,
                          ClassWithClassAnnotations.GroupAnnotationOnly.class,
                          ClassWithClassAnnotations.SecondClassGroupOnly_GroupAnnotationWithoutName.class,
                          ClassWithMethodAnnotations.class,
                          ClassWithMethodAnnotations.MethodGroupOnlyAnon.class
                };
        Node<RateConfig> root = getInstance().processAll(classes);
        System.out.println(root);
        assertThat(root.findFirstChild(node -> node.getName().equals(root.getName())).isPresent()).isTrue();
        assertHasChildrenHavingNames(root, "ClassGroupOnlyAnon", "PrivateClass", "InternalClass");
        assertHasChildrenHavingNames(root, "GroupAnnotationWithoutName");
        Node<RateConfig> fire = root.findFirstChild(node -> getId(GroupAnnotationWithoutName.class).equals(node.getName()))
                .orElseThrow(NullPointerException::new);
        assertHasChildrenHavingNames(fire,
                ClassWithClassAnnotations.ClassGroupOnly_GroupAnnotationWithoutName.class,
                ClassWithClassAnnotations.SecondClassGroupOnly_GroupAnnotationWithoutName.class);
    }

    RateProcessor<Class<?>> getInstance() {
        return RateProcessor.ofDefaults();
    }

    @Override String getId(Class<?> element) {
        return ElementId.of(element);
    }

    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    @RateGroup(operator = Operator.AND)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    public @interface GroupAnnotationWithoutName { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @RateGroup("x")
    public @interface GroupAnnotationsOnlyGroup { }

    public static class ClassWithClassAnnotations {

        @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @Rate(permits = 10, timeUnit = MILLISECONDS)
        public class ClassGroupOnlyAnon { }

        @GroupAnnotationWithoutName
        public class ClassGroupOnly_GroupAnnotationWithoutName { }

        public class ClassWithInternalClass {
            @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
            @Rate(permits = 10, timeUnit = MILLISECONDS)
            public class InternalClass{ }
        }


        @GroupAnnotationsOnlyGroup
        public class GroupAnnotationOnly { }

        @GroupAnnotationWithoutName
        public class SecondClassGroupOnly_GroupAnnotationWithoutName { }

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

        @GroupAnnotationWithoutName
        void methodGroupOnly_groupAnnotationWithoutName() { }
    }
}