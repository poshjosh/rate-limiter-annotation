package io.github.poshjosh.ratelimiter.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassesInPackageFinderTest {

    @Test 
    void findClasses() {
        final List<Class<?>> expected = Arrays.asList(
              ClassesInPackageFinderTest.ClassWithClassAnnotations.class,
              ClassesInPackageFinderTest.ClassWithClassAnnotations.ClassGroupOnlyAnon.class,
              ClassesInPackageFinderTest.ClassWithClassAnnotations.ClassGroupOnlyNamedFire.class,
              ClassesInPackageFinderTest.ClassWithClassAnnotations.ClassWithInternalClass.class,
              ClassesInPackageFinderTest.ClassWithClassAnnotations.ClassWithInternalClass.InternalClass.class,
              ClassesInPackageFinderTest.ClassWithClassAnnotations.PrivateClass.class,
              ClassesInPackageFinderTest.ClassWithClassAnnotations.GroupAnnotationOnly.class,
              ClassesInPackageFinderTest.ClassWithClassAnnotations.SecondClassGroupOnlyNamedFire.class,
              ClassesInPackageFinderTest.ClassWithMethodAnnotations.class,
              ClassesInPackageFinderTest.ClassWithMethodAnnotations.MethodGroupOnlyAnon.class
        );
      
        final List<Class<?>> found = ClassesInPackageFinder.ofDefaults()
                .findClasses(getClass().getPackage().getName(), clz -> true);
        assertTrue(found.containsAll(expected));
    }


  public static class ClassWithClassAnnotations {

    public class ClassGroupOnlyAnon { }

    public class ClassGroupOnlyNamedFire { }

    public class ClassWithInternalClass {
      public class InternalClass{ }
    }

    public class GroupAnnotationOnly { }

    public class SecondClassGroupOnlyNamedFire { }

    class PrivateClass{ }
  }

  public static class ClassWithMethodAnnotations {
    public class MethodGroupOnlyAnon {
      void anon() { }
    }
    void fire() { }
  }
}