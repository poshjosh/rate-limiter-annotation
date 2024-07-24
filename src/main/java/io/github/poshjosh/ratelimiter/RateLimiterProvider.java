package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;

public interface RateLimiterProvider {
    static RateLimiterProvider ofDefaults() {
        return RateLimiterProviders.ofDefaults();
    }

    RateLimiter getRateLimiter(String key, Rate rate);

    RateLimiter getRateLimiter(String key, Rates rates);
}
