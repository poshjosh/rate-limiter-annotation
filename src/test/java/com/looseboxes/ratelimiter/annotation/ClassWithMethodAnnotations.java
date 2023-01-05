package com.looseboxes.ratelimiter.annotation;

import com.looseboxes.ratelimiter.annotations.Rate;
import com.looseboxes.ratelimiter.annotations.RateGroup;
import com.looseboxes.ratelimiter.util.Operator;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ClassWithMethodAnnotations {
    public class MethodGroupOnlyAnon {
        @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
        @Rate(permits = 10, timeUnit = MILLISECONDS)
        void anon() { }
    }

    @RateGroup(name = "Fire", operator = Operator.AND)
    @Rate(permits = 2, duration = 20, timeUnit = MILLISECONDS)
    @Rate(permits = 10, timeUnit = MILLISECONDS)
    void fire() { }
}
