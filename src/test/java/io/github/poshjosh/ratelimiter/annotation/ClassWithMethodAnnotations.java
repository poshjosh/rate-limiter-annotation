package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.util.Operator;

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
