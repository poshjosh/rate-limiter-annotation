package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.util.LimiterConfig;

import java.util.Objects;

public interface UsageListener {

    UsageListener NO_OP = new UsageListener() {
        @Override public String toString() { return "UsageListener$NO_OP"; }
    };

    default void onConsumed(Object request, String resourceId, int permits, LimiterConfig<?> config) { }

    default void onRejected(Object request, String resourceId, int permits, LimiterConfig<?> config) { }

    /**
     * Returns a composed {@code UsageListener} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code UsageListener} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default UsageListener andThen(UsageListener after) {
        Objects.requireNonNull(after);
        return new UsageListener() {
            @Override
            public void onConsumed(Object request, String resourceId, int permits, LimiterConfig<?> config) {
                UsageListener.this.onConsumed(request, resourceId, permits, config);
                after.onConsumed(request, resourceId, permits, config);
            }
            @Override
            public void onRejected(Object request, String resourceId, int permits, LimiterConfig<?> config) {
                UsageListener.this.onRejected(request, resourceId, permits, config);
                after.onRejected(request, resourceId, permits, config);
            }
            @Override public String toString() {
                return "UsageListener$andThen{first=" + this + ", after=" + after + "}";
            }
        };
    }
}
