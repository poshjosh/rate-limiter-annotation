package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.util.Operator;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RateGroup {

    /** Alias for value() */
    String name() default "";

    /** Alias for name() */
    String value() default "";

    Operator operator() default Operator.OR;
}
