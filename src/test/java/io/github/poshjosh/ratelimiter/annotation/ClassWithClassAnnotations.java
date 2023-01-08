package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.util.Operator;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ClassWithClassAnnotations {

    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    public class ClassGroupOnlyAnon { }

    //    @RateGroup("Fire")
    @RateGroup(name = "Fire", operator = Operator.AND)
    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    public class ClassGroupOnlyNamedFire { }

    public class ClassWithInternalClass {
        @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @Rate(permits = 10, timeUnit = MILLISECONDS)
        public class InternalClass{ }
    }

    @RateGroup
    public class GroupAnnotationOnly { }

    @RateGroup(name = "Fire", operator = Operator.AND)
    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    public class SecondClassGroupOnlyNamedFire { }

    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    class PrivateClass{ }
}
