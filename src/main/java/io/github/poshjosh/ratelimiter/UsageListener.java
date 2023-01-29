package io.github.poshjosh.ratelimiter;

import java.util.Objects;

public interface UsageListener {

    UsageListener NO_OP = new UsageListener() { };

    default void onConsumed(Object resource, int hits, Object limit) { }

    default void onRejected(Object resource, int hits, Object limit) { }

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
            public void onConsumed(Object resource, int hits, Object limit) {
                UsageListener.this.onConsumed(resource, hits, limit);
                after.onConsumed(resource, hits, limit);
            }
            @Override
            public void onRejected(Object resource, int hits, Object limit) {
                UsageListener.this.onRejected(resource, hits, limit);
                after.onRejected(resource, hits, limit);
            }
        };
    }
}
