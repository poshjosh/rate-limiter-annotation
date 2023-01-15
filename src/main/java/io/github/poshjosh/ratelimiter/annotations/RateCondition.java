package io.github.poshjosh.ratelimiter.annotations;

import java.lang.annotation.*;

/**
 * Holds an expression which specifies the condition for rate limiting.
 *
 * May be any supported string for example:
 *
 * <p><code>sys.memory.available<1_000_000_000</code></p>
 * <p><code>web.request.user.role=ROLE_GUEST</code></p>
 *
 * Support must be provide for the expression. Support is provided by default for the following:
 *
 * <p><code>sys.memory.available</code></p>
 * <p><code>sys.memory.free</code></p>
 * <p><code>sys.memory.max</code></p>
 * <p><code>sys.memory.total</code></p>
 * <p><code>sys.memory.used</code></p>
 * <p><code>sys.time.elapsed</code></p>
 * <p><code>sys.time</code></p>
 *
 * Supported operators are:
 *
 * <pre>
 * =  equals
 * >  greater
 * >= greater or equals
 * <  less
 * <= less or equals
 * ^  starts with
 * $  ends with
 * %  contains
 * !  not (e.g !=, !>, !$ etc)
 * </pre>
 *
 * @see io.github.poshjosh.ratelimiter.matcher.ExpressionResolver
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD})
public @interface RateCondition {

    /** Alias for value() */
    String expression() default "";

    /** Alias for expression() */
    String value() default "";
}
