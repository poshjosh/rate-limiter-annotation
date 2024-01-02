package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.util.Rate;
import io.github.poshjosh.ratelimiter.util.Rates;

import java.util.List;

public interface RateToBandwidthConverter{

    static RateToBandwidthConverter ofDefaults() {
        return (rate, nowMicros) -> {
            BandwidthFactory factory = BandwidthFactories.getOrCreateBandwidthFactory(rate.getFactoryClass());
            return factory.createNew(rate.getPermits(), rate.getDuration(), nowMicros);
        };
    }

    Bandwidth convert(Rate rate, long nowMicros);

    default Bandwidth[] convert(String id, Rates rates, long nowMicros) {
        if (!rates.hasLimits()) {
            return new Bandwidth[0];
        }
        final List<Rate> limits = rates.getLimits();
        final Bandwidth[] bandwidths = new Bandwidth[limits.size()];
        for (int i = 0; i < bandwidths.length; i++) {
            Rate rate = limits.get(i);
            bandwidths[i] = convert(rate, nowMicros);
        }
        if (bandwidths.length == 1 || rates.hasChildConditions()) {
            // We ignore the operator
            // see Tag:Rule:Operator-may-not-be-specified-when-multiple-rate-conditions-are-specified
            return bandwidths;
        }
        final Operator operator =
                Operator.NONE.equals(rates.getOperator()) ? Operator.OR : rates.getOperator();
        return new Bandwidth[]{Bandwidths.of(id, operator, bandwidths)};
    }
}
