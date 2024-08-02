package io.github.poshjosh.ratelimiter.annotations;

import io.github.poshjosh.ratelimiter.bandwidths.BandwidthFactories;
import io.github.poshjosh.ratelimiter.bandwidths.BandwidthFactory;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Repeatable(Rate.List.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Rate {

    String id() default "";

    /** Alias for value */
    long permits() default Long.MAX_VALUE;

    /** Alias for permits */
    long value() default Long.MAX_VALUE;

    long duration() default 1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * Holds an expression which specifies the condition for rate limiting.
     *
     * <p>Alias for {@link #when()}</p>
     *
     * May be any supported string for example:
     *
     * <p><code>jvm.memory.available < 1_000_000_000</code></p>
     * <p><code>web.request.user.role = ROLE_GUEST</code></p>
     *
     * Support must be provide for the expression. Support is provided by default for the following:
     *
     * <p><code>jvm.thread.count < /code></p>
     * <p><code>jvm.thread.count.daemon < /code></p>
     * <p><code>jvm.thread.count.deadlocked < /code></p>
     * <p><code>jvm.thread.count.deadlocked.monitor < /code></p>
     * <p><code>jvm.thread.count.peak < /code></p>
     * <p><code>jvm.thread.count.started < /code></p>
     * <p><code>jvm.thread.current.count.blocked < /code></p>
     * <p><code>jvm.thread.current.count.waited < /code></p>
     * <p><code>jvm.thread.current.id < /code></p>
     * <p><code>jvm.thread.current.state < /code></p>
     * <p><code>jvm.thread.current.suspended < /code></p>
     * <p><code>jvm.thread.current.time.blocked < /code></p>
     * <p><code>jvm.thread.current.time.cpu < /code></p>
     * <p><code>jvm.thread.current.time.user < /code></p>
     * <p><code>jvm.thread.current.time.waited < /code></p>
     * <p><code>jvm.memory.available < /code></p>
     * <p><code>jvm.memory.free < /code></p>
     * <p><code>jvm.memory.max < /code></p>
     * <p><code>jvm.memory.total < /code></p>
     * <p><code>jvm.memory.used < /code></p>
     * <p><code>sys.time.elapsed < /code></p>
     * <p><code>sys.time < /code></p>
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
     * @see #when()
     * @see io.github.poshjosh.ratelimiter.expression.ExpressionResolver
     */
    String condition() default "";

    /**
     * Alias for {@link #condition()}
     * @see #condition()
     */
    String when() default "";

    /**
     * A {@link BandwidthFactory} that will be dynamically instantiated and used to create
     * {@link Bandwidth}s from this rate limit.
     * The class must have a zero-argument constructor.
     *
     * @return a {@link BandwidthFactory} class for creating {@link Bandwidth}s
     */
    Class<? extends BandwidthFactory> factoryClass() default BandwidthFactories.Default.class;

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface List {
        Rate[] value();
    }
}
