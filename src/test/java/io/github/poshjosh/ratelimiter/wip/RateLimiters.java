package io.github.poshjosh.ratelimiter.wip;

import io.github.poshjosh.ratelimiter.RateLimiter;
import io.github.poshjosh.ratelimiter.RateToBandwidthConverter;
import io.github.poshjosh.ratelimiter.annotation.RateProcessor;
import io.github.poshjosh.ratelimiter.bandwidths.Bandwidth;
import io.github.poshjosh.ratelimiter.node.Node;
import io.github.poshjosh.ratelimiter.util.RateConfig;
import io.github.poshjosh.ratelimiter.util.Rates;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class RateLimiters {
    private RateLimiters() { }

    public static List<RateLimiter> create(Class<?> source) {
        return create(RateProcessor.ofClass(), source);
    }

    public static List<RateLimiter> create(Method source) {
        return create(RateProcessor.ofMethod(), source);
    }

    private static <S> List<RateLimiter> create(RateProcessor<S> rateProcessor, S source) {
        Node<RateConfig> node = rateProcessor.process(source);
        RateConfig rateConfig = node.getValueOrDefault(null);
        if (rateConfig == null) {
            return Collections.emptyList();
        }
        Rates rates = rateConfig.getRates();
        if (!rates.hasLimits()) {
            return Collections.emptyList();
        }
        Bandwidth [] bandwidths = RateToBandwidthConverter.ofDefaults()
                .convert(node.getName(), rates, 0);
        return Arrays.stream(bandwidths).map(RateLimiter::of).collect(Collectors.toList());
    }
}
