package com.looseboxes.ratelimiter.annotations;

import com.looseboxes.ratelimiter.BandwidthFactory;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Repeatable(RateLimit.List.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RateLimit {

    long permits() default Long.MAX_VALUE;

    long duration() default 0;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * A {@link BandwidthFactory} that will be dynamically instantiated and used to create
     * {@link com.looseboxes.ratelimiter.bandwidths.Bandwidth}s from this rate limit.
     * The class must have a zero-argument constructor.
     *
     * @return a {@link BandwidthFactory} class for creating {@link com.looseboxes.ratelimiter.bandwidths.Bandwidth}s
     */
    Class<? extends BandwidthFactory> factoryClass() default BandwidthFactory.Default.class;

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface List {
        RateLimit[] value();
    }
}
