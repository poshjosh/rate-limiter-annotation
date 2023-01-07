package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidths;
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

    default Bandwidths convert(Rates rates) {
        return convert(rates, 0);
    }

    default Bandwidths convert(Rates rates, long nowMicros) {
        final List<Rate> limits = rates.getLimits();
        if (limits == null || limits.isEmpty()) {
            return Bandwidths.empty(rates.getOperator());
        }
        Bandwidth[] members = new Bandwidth[limits.size()];
        for (int i = 0; i < members.length; i++) {
            members[i] = convert(limits.get(i), nowMicros);
        }
        return Bandwidths.of(rates.getOperator(), members);
    }

    default Bandwidth convert(Rate rate) {
        return convert(rate, 0);
    }
}
