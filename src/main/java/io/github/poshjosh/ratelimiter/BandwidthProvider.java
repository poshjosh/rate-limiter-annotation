package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;

public interface BandwidthProvider {
    static BandwidthProvider ofDefaults() {
        return RateLimiterProviders.ofDefaults();
    }

    Bandwidth getBandwidth(String key, Rate rate);

    Bandwidth getBandwidth(String key, Rates rates);
}
