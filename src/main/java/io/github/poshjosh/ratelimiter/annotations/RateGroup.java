package io.github.poshjosh.ratelimiter.annotations;

import io.github.poshjosh.ratelimiter.util.Operator;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface RateGroup {

    /** Alias for value() */
    String id() default "";

    /** Alias for name() */
    String value() default "";

    Operator operator() default Operator.NONE;
}
