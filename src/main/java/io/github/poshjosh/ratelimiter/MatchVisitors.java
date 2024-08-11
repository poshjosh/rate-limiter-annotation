package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.util.Ticker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MatchVisitors {

    static MatchContext.MatchVisitor<Double> permitAcquiring(
            RateLimiterProvider rateLimiterProvider, int permits) {
        return new PermitAcquiringVisitor(rateLimiterProvider, permits);
    }

    static MatchContext.MatchVisitor<Boolean> permitAttempting(
            RateLimiterProvider rateLimiterProvider, int permits, long timeout, TimeUnit timeUnit) {
        return new PermitAttemptingVisitor(rateLimiterProvider, permits, timeout, timeUnit);
    }

    static MatchContext.MatchVisitor<Boolean> limitChecking(
            RateLimiterProvider rateLimiterProvider, Ticker ticker) {
        return new LimitCheckingVisitor(rateLimiterProvider, ticker);
    }

    static MatchContext.MatchVisitor<Bandwidth> bandwidthCollecting(
            RateLimiterProvider rateLimiterProvider) {
        return new BandwidthCollectingVisitor(rateLimiterProvider);
    }

    private MatchVisitors() { }

    private static final class PermitAcquiringVisitor extends MatchingRateLimiterVisitor<Double> {

        private final int permits;

        private double totalTimeSpent = 0;

        private PermitAcquiringVisitor(RateLimiterProvider rateLimiterProvider, int permits) {
            super(rateLimiterProvider);
            this.permits = permits;
        }

        @Override
        protected void visit(String match, RateLimiter rateLimiter) {
            double timeSpent = rateLimiter.acquire(permits);
            if (timeSpent > 0) { // Only increment when > 0, as some value may be negative.
                totalTimeSpent += timeSpent;
            }
        }

        @Override
        public Double getResult() {
            return totalTimeSpent;
        }
    }

    private static final class PermitAttemptingVisitor extends MatchingRateLimiterVisitor<Boolean> {

        private final int permits;

        private final long timeout;

        private final TimeUnit timeUnit;

        private boolean noLimitExceeded = true;

        private PermitAttemptingVisitor(
                RateLimiterProvider rateLimiterProvider, int permits, long timeout, TimeUnit timeUnit) {
            super(rateLimiterProvider);
            this.permits = permits;
            this.timeout = timeout;
            this.timeUnit = Objects.requireNonNull(timeUnit);
        }

        @Override
        protected void visit(String match, RateLimiter rateLimiter) {
            if (!rateLimiter.tryAcquire(permits, timeout, timeUnit)) {
                noLimitExceeded = false;
            }
        }

        @Override
        public Boolean getResult() {
            return noLimitExceeded;
        }
    }

    private static final class LimitCheckingVisitor
            implements MatchContext.MatchVisitor<Boolean> {
        private boolean limitExceeded = false;

        private final RateLimiterProvider rateLimiterProvider;
        private final Ticker ticker;
        private LimitCheckingVisitor(RateLimiterProvider rateLimiterProvider, Ticker tiker) {
            this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
            this.ticker = Objects.requireNonNull(tiker);
        }

        @Override public void visit(String match, Rate rate) {
            final Bandwidth bandwidth = rateLimiterProvider.getBandwidth(match, rate);
            if (!bandwidth.isAvailable(ticker.elapsedMicros())) {
                limitExceeded = true;
            }
        }

        @Override public void visit(String match, Rates rates) {
            final Bandwidth bandwidth = rateLimiterProvider.getBandwidth(match, rates);
            if (!bandwidth.isAvailable(ticker.elapsedMicros())) {
                limitExceeded = true;
            }
        }

        @Override
        public Boolean getResult() {
            return !limitExceeded;
        }
    }

    private static final class BandwidthCollectingVisitor
            implements MatchContext.MatchVisitor<Bandwidth> {
        private final List<Bandwidth> bandwidths = new ArrayList<>();

        private final RateLimiterProvider rateLimiterProvider;
        private BandwidthCollectingVisitor(RateLimiterProvider rateLimiterProvider) {
            this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
        }

        @Override public void visit(String match, Rate rate) {
            bandwidths.add(rateLimiterProvider.getBandwidth(match, rate));
        }

        @Override public void visit(String match, Rates rates) {
            bandwidths.add(rateLimiterProvider.getBandwidth(match, rates));
        }

        @Override
        public Bandwidth getResult() {
            // For multiple Bandwidths conjugated with Operator.OR, the composed Bandwidth
            // succeeds only when all Bandwidths succeed. This is the case here.
            return Bandwidths.of(Operator.OR, bandwidths.toArray(new Bandwidth[0]));
        }
    }

    abstract static class MatchingRateLimiterVisitor<R> implements MatchContext.MatchVisitor<R> {

        private final RateLimiterProvider rateLimiterProvider;
        MatchingRateLimiterVisitor(RateLimiterProvider rateLimiterProvider) {
            this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
        }

        protected abstract void visit(String match, RateLimiter rateLimiter);

        @Override
        public R getResult() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(String match, Rate rate) {
            visit(match, rateLimiterProvider.getRateLimiter(match, rate));
        }

        @Override
        public void visit(String match, Rates rates) {
            visit(match, rateLimiterProvider.getRateLimiter(match, rates));
        }
    }
}
