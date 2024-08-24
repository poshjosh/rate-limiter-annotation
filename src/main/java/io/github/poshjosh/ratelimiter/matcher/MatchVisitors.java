package io.github.poshjosh.ratelimiter.matcher;

import io.github.poshjosh.ratelimiter.RateLimiter;
import io.github.poshjosh.ratelimiter.RateLimiterProvider;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.model.Operator;
import io.github.poshjosh.ratelimiter.util.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MatchVisitors {

    private static final Logger LOG = LoggerFactory.getLogger(MatchVisitors.class);

    public static MatchVisitor<Double> permitAcquiring(
            RateLimiterProvider rateLimiterProvider, int permits) {
        return new PermitAcquiringVisitor(rateLimiterProvider, permits);
    }

    public static MatchVisitor<Boolean> permitAttempting(
            RateLimiterProvider rateLimiterProvider, int permits, long timeout, TimeUnit timeUnit) {
        return new PermitAttemptingVisitor(rateLimiterProvider, permits, timeout, timeUnit);
    }

    public static MatchVisitor<Boolean> limitChecking(
            RateLimiterProvider rateLimiterProvider, Ticker ticker) {
        return new LimitCheckingVisitor(rateLimiterProvider, ticker);
    }

    public static MatchVisitor<Bandwidth> bandwidthCollecting(
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
            if (LOG.isTraceEnabled()) {
                LOG.trace("For: {}, acquired {} permits in {} seconds (Accumulated: {} seconds) from: {}",
                        match, permits, timeSpent, totalTimeSpent, rateLimiter);
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
            final boolean acquired = rateLimiter.tryAcquire(permits, timeout, timeUnit);
            if (!acquired) {
                noLimitExceeded = false;
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace("For: {}, acquired: {}, {} permits (anyLimitExceeded: {}), with timeout: {} {}, from: {}",
                        match, acquired, permits, !noLimitExceeded, timeout, timeUnit, rateLimiter);
            }
        }

        @Override
        public Boolean getResult() {
            return noLimitExceeded;
        }
    }

    private static final class LimitCheckingVisitor
            implements MatchVisitor<Boolean> {
        private boolean anyLimitExceeded = false;

        private final RateLimiterProvider rateLimiterProvider;
        private final Ticker ticker;
        private LimitCheckingVisitor(RateLimiterProvider rateLimiterProvider, Ticker ticker) {
            this.rateLimiterProvider = Objects.requireNonNull(rateLimiterProvider);
            this.ticker = Objects.requireNonNull(ticker);
        }

        @Override public void visit(String match, Rate rate) {
            final Bandwidth bandwidth = rateLimiterProvider.getBandwidth(match, rate);
            visit(match, rate, bandwidth);
        }

        @Override public void visit(String match, Rates rates) {
            final Bandwidth bandwidth = rateLimiterProvider.getBandwidth(match, rates);
            visit(match, rates, bandwidth);
        }

        private void visit(String match, Object rates, Bandwidth bandwidth) {
            final boolean available = bandwidth.isAvailable(ticker.elapsedMicros());
            if (!available) {
                anyLimitExceeded = true;
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace("For: {}, is available: {} (anyLimitExceeded: {}), for: {} of: {}",
                        match, available, anyLimitExceeded, bandwidth, rates);
            }
        }

        @Override
        public Boolean getResult() {
            return !anyLimitExceeded;
        }
    }

    private static final class BandwidthCollectingVisitor
            implements MatchVisitor<Bandwidth> {
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

    public abstract static class MatchingRateLimiterVisitor<R> implements MatchVisitor<R> {

        private final RateLimiterProvider rateLimiterProvider;
        protected MatchingRateLimiterVisitor(RateLimiterProvider rateLimiterProvider) {
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
