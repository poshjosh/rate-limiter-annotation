package io.github.poshjosh.ratelimiter.performance.dummyclasses;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Rate(1)
@Rate(permits = 10, timeUnit = TimeUnit.MINUTES)
@RateCondition("jvm.memory.available > 100MB")
@RateGroup("rate-group-A")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface RateGroupA { }
