package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.BandwidthFactory;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;

import java.time.Duration;
import java.util.Objects;

public final class Rate {

    public static Rate ofNanos(long permits) {
        return of(permits, Duration.ofNanos(1));
    }

    public static Rate ofMillis(long permits) {
        return of(permits, Duration.ofMillis(1));
    }

    public static Rate ofSeconds(long permits) {
        return of(permits, Duration.ofSeconds(1));
    }

    public static Rate ofMinutes(long permits) {
        return of(permits, Duration.ofMinutes(1));
    }

    public static Rate ofHours(long permits) {
        return of(permits, Duration.ofHours(1));
    }

    public static Rate ofDays(long permits) {
        return of(permits, Duration.ofDays(1));
    }

    public static Rate of(long permits, String rateCondition) {
        return of(permits, Duration.ofSeconds(1), rateCondition);
    }

    public static Rate of(long permits, Duration duration) {
        return of(permits, duration, "");
    }

    public static Rate of(long permits, Duration duration, String rateCondition) {
        return of(permits, duration, rateCondition, BandwidthFactory.Default.class);
    }

    public static Rate of(long permits, Duration duration,
            String rateCondition, Class<? extends BandwidthFactory> factoryClass) {
        return new Rate(permits, duration, rateCondition, factoryClass);
    }

    public static Rate of(Rate rate) {
        return new Rate(rate);
    }

    private long permits;
    private Duration duration = Duration.ofSeconds(1);


    /**
     * An expression which specifies the condition for rate limiting.
     *
     * May be any supported string for example:
     *
     * <p><code>sys.memory.available<1_000_000_000</code></p>
     * <p><code>web.request.user.role=ROLE_GUEST</code></p>
     *
     * Support must be provide for the expression. Support is provided by default for the following:
     *
     * <p><code>jvm.thread.count</code></p>
     * <p><code>jvm.thread.count.daemon</code></p>
     * <p><code>jvm.thread.count.deadlocked</code></p>
     * <p><code>jvm.thread.count.deadlocked.monitor</code></p>
     * <p><code>jvm.thread.count.peak</code></p>
     * <p><code>jvm.thread.count.started</code></p>
     * <p><code>jvm.thread.current.count.blocked</code></p>
     * <p><code>jvm.thread.current.count.waited</code></p>
     * <p><code>jvm.thread.current.state</code></p>
     * <p><code>jvm.thread.current.suspended</code></p>
     * <p><code>jvm.thread.current.time.blocked</code></p>
     * <p><code>jvm.thread.current.time.cpu</code></p>
     * <p><code>jvm.thread.current.time.user</code></p>
     * <p><code>jvm.thread.current.time.waited</code></p>
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
     * @see io.github.poshjosh.ratelimiter.expression.ExpressionResolver
     * @see Rates#getRateCondition()
     */
    private String rateCondition = "";

    /**
     * A {@link BandwidthFactory} that will be dynamically instantiated and used to create
     * {@link Bandwidth}s from this rate limit.
     * The class must have a zero-argument constructor.
     */
    private Class<? extends BandwidthFactory> factoryClass = BandwidthFactory.Default.class;

    public Rate() { }

    Rate(Rate rate) {
        this(rate.permits, rate.duration, rate.rateCondition, rate.factoryClass);
    }

    Rate(long permits, Duration duration, String rateCondition,
            Class<? extends BandwidthFactory> factoryClass) {
        requireNotNegative(permits, "permits");
        requireFalse(duration.isNegative(), "Duration must be withPositiveOperator, duration: " + duration);
        this.permits = permits;
        this.duration = Objects.requireNonNull(duration);
        this.rateCondition = Objects.requireNonNull(rateCondition);
        this.factoryClass = Objects.requireNonNull(factoryClass);
    }
    private void requireNotNegative(double amount, String what) {
        requireFalse(amount < 0, "Must not be negative, %s: %d", what, amount);
    }
    private void requireFalse(boolean expression, String errorMessageFormat, Object... args) {
        if (expression) {
            throw new IllegalArgumentException(String.format(errorMessageFormat, args));
        }
    }

    public boolean isSet() {
        return permits > 0 && duration != null;
    }

    public Rate permits(long permits) {
        this.setPermits(permits);
        return this;
    }

    public long getPermits() {
        return permits;
    }

    public void setPermits(long permits) {
        this.permits = permits;
    }

    public Rate duration(Duration duration) {
        this.setDuration(duration);
        return this;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public Rate rateCondition(String rateCondition) {
        setRateCondition(rateCondition);
        return this;
    }

    public String getRateCondition() {
        return rateCondition;
    }

    public void setRateCondition(String rateCondition) {
        this.rateCondition = rateCondition;
    }

    public Rate factoryClass(Class<? extends BandwidthFactory> factoryClass) {
        setFactoryClass(factoryClass);
        return this;
    }

    public Class<? extends BandwidthFactory> getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(Class<? extends BandwidthFactory> factoryClass) {
        this.factoryClass = factoryClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rate that = (Rate) o;
        return permits == that.permits && duration.equals(that.duration)
                && rateCondition.equals(that.rateCondition) && factoryClass.equals(that.factoryClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permits, duration, rateCondition, factoryClass);
    }

    @Override
    public String toString() {
        return "Rate{" +
                "permits=" + permits +
                ", duration=" + duration +
                ", condition=" + rateCondition +
                ", factoryClass=" + (factoryClass == null ? null : factoryClass.getSimpleName()) +
                '}';
    }
}
