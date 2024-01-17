package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RateLimiterRegistryTest {

    private static final String ID = "test";

    @Test
    void shouldCreateRateLimiterWhenOnlyPackagesSpecified() {
        RateLimiterContext<Object> context = RateLimiterContext.builder()
                .packages("dummy-package")
                .build();
        RateLimiterFactory<Object> factory = RateLimiterRegistry.of(context).createRateLimiterFactory();
        assertNotNull(factory);
        assertNotNull(factory.getRateLimiter(ID));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyClassesSpecified() {
        RateLimiterContext<Object> context = RateLimiterContext.builder()
                .classes(RateLimiterRegistryTest.class)
                .build();
        RateLimiterFactory<Object> factory = RateLimiterRegistry.of(context).createRateLimiterFactory();
        assertNotNull(factory);
        assertNotNull(factory.getRateLimiter(ID));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyRatesSpecified() {
        RateLimiterContext<Object> context = RateLimiterContext.builder()
                .rates(Collections.singletonMap(ID, Rates.of(Rate.ofSeconds(1))))
                .build();
        RateLimiterFactory<Object> factory = RateLimiterRegistry.of(context).createRateLimiterFactory();
        assertNotNull(factory);
        assertNotNull(factory.getRateLimiter(ID));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyPropertiesSpecified() {
        RateLimitProperties properties = new RateLimitProperties() {
            @Override public List<Class<?>> getResourceClasses() {
                return Collections.emptyList();
            }
            @Override public List<String> getResourcePackages() { return Arrays.asList("package"); }
        };
        RateLimiterContext<Object> context = RateLimiterContext.builder()
                .properties(properties)
                .build();
        RateLimiterFactory<Object> factory = RateLimiterRegistry.of(context).createRateLimiterFactory();
        assertNotNull(factory);
        assertNotNull(factory.getRateLimiter(ID));
    }
}