package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.RateLimit;
import com.looseboxes.ratelimiter.annotations.RateLimitGroup;
import com.looseboxes.ratelimiter.util.Operator;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ClassWithMethodAnnotations {
    public class MethodGroupOnlyAnon {
        @RateLimit(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @RateLimit(permits = 1, duration = 10, timeUnit = MILLISECONDS)
        void anon() { }
    }

    @RateLimitGroup(name = "Fire", operator = Operator.AND)
    @RateLimit(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @RateLimit(permits = 1, duration = 10, timeUnit = MILLISECONDS)
    void fire() { }
}
