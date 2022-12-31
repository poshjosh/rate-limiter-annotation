package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.RateLimit;
import com.looseboxes.ratelimiter.annotations.RateLimitGroup;
import com.looseboxes.ratelimiter.util.Operator;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ClassWithClassAnnotations {

    @RateLimit(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @RateLimit(permits = 1, duration = 10, timeUnit = MILLISECONDS)
    public class ClassGroupOnlyAnon { }

    //    @RateLimitGroup("Fire")
    @RateLimitGroup(name = "Fire", operator = Operator.AND)
    @RateLimit(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @RateLimit(permits = 1, duration = 10, timeUnit = MILLISECONDS)
    public class ClassGroupOnlyNamedFire { }

    public class ClassWithInternalClass {
        @RateLimit(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @RateLimit(permits = 1, duration = 10, timeUnit = MILLISECONDS)
        public class InternalClass{ }
    }

    @RateLimitGroup
    public class GroupAnnotationOnly { }

    @RateLimitGroup(name = "Fire", operator = Operator.AND)
    @RateLimit(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @RateLimit(permits = 1, duration = 10, timeUnit = MILLISECONDS)
    public class SecondClassGroupOnlyNamedFire { }

    @RateLimit(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @RateLimit(permits = 1, duration = 10, timeUnit = MILLISECONDS)
    class PrivateClass{ }
}
