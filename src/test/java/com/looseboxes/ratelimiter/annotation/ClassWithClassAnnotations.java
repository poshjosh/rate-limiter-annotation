package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.Rate;
import com.looseboxes.ratelimiter.annotations.RateGroup;
import com.looseboxes.ratelimiter.util.Operator;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ClassWithClassAnnotations {

    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 1, duration = 10, timeUnit = MILLISECONDS)
    public class ClassGroupOnlyAnon { }

    //    @RateGroup("Fire")
    @RateGroup(name = "Fire", operator = Operator.AND)
    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 1, duration = 10, timeUnit = MILLISECONDS)
    public class ClassGroupOnlyNamedFire { }

    public class ClassWithInternalClass {
        @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @Rate(permits = 1, duration = 10, timeUnit = MILLISECONDS)
        public class InternalClass{ }
    }

    @RateGroup
    public class GroupAnnotationOnly { }

    @RateGroup(name = "Fire", operator = Operator.AND)
    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 1, duration = 10, timeUnit = MILLISECONDS)
    public class SecondClassGroupOnlyNamedFire { }

    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 1, duration = 10, timeUnit = MILLISECONDS)
    class PrivateClass{ }
}
