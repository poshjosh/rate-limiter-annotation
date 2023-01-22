package io.github.poshjosh.ratelimiter.annotations;

import io.github.poshjosh.ratelimiter.BandwidthFactory;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Repeatable(Rate.List.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Rate {

    String name() default "";

    /** Alias for value */
    long permits() default Long.MAX_VALUE;

    /** Alias for permits */
    long value() default Long.MAX_VALUE;

    long duration() default 1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * A {@link BandwidthFactory} that will be dynamically instantiated and used to create
     * {@link Bandwidth}s from this rate limit.
     * The class must have a zero-argument constructor.
     *
     * @return a {@link BandwidthFactory} class for creating {@link Bandwidth}s
     */
    Class<? extends BandwidthFactory> factoryClass() default BandwidthFactory.Default.class;

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface List {
        Rate[] value();
    }
}
