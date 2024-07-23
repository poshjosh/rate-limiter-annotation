package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.bandwidths.BandwidthFactory;
import io.github.poshjosh.ratelimiter.model.Rate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterRegistryTest2 {

    final String key = "0";

    final int durationMillis = 2000;

    private final Class<? extends BandwidthFactory> factoryClass = BandwidthFactory.AllOrNothing.class;
    private final boolean supportsNullKeys = true;

    @io.github.poshjosh.ratelimiter.annotations.Rate(1)
    static class ResourceWithClassAndMethodRates {
        @io.github.poshjosh.ratelimiter.annotations.Rate(id = "smile", permits = 2)
        void smile() { }
        static Method getRateLimitedMethod() {
            try {
                return ResourceWithClassAndMethodRates.class.getDeclaredMethod("smile");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void shouldReturnClassOnlyRateLimiterGivenClass() {
        RateLimiter limiter = RateLimiterRegistry.getLimiter(ResourceWithClassAndMethodRates.class);
        assertThat(limiter).isNotNull();
        assertThat(limiter.tryAcquire(1)).isTrue();
        assertThat(limiter.tryAcquire(1)).isFalse();
    }

    @Test
    void shouldReturnMethodAndDeclaringClassRateLimiterGivenMethod() {
        RateLimiter limiter = RateLimiterRegistry.getLimiter(
                ResourceWithClassAndMethodRates.class, ResourceWithClassAndMethodRates.getRateLimitedMethod());
        assertThat(limiter).isNotNull();
        assertThat(limiter.tryAcquire(1)).isTrue();
        assertThat(limiter.tryAcquire(1)).isFalse();
    }

    @Test
    void shouldReturnMethodOnlyRateLimiterGivenMethodRateId() {
        RateLimiter limiter = RateLimiterRegistry.getLimiter(
                ResourceWithClassAndMethodRates.class, "smile");
        assertThat(limiter).isNotNull();
        assertThat(limiter.tryAcquire(2)).isTrue();
        assertThat(limiter.tryAcquire(1)).isFalse();
    }

    @io.github.poshjosh.ratelimiter.annotations.Rate(2)
    static class ResourceWithClassRateLargerThanMethodRate {
        @io.github.poshjosh.ratelimiter.annotations.Rate(id = "smile", permits = 1)
        void smile() { }
        static Method getRateLimitedMethod() {
            try {
                return ResourceWithClassRateLargerThanMethodRate.class.getDeclaredMethod("smile");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // The rate limiter applies to the method and its declaring class
    // However, since the method's limit is lower, it triggers rate
    // limiting before the class limit is reached
    @Test
    void shouldUseMethodRateGivenMethodOfResourceWithClassRateLargerThanMethodRate() {
        RateLimiter limiter = RateLimiterRegistry.getLimiter(
                ResourceWithClassRateLargerThanMethodRate.class,
                ResourceWithClassRateLargerThanMethodRate.getRateLimitedMethod());
        assertThat(limiter).isNotNull();
        assertThat(limiter.tryAcquire(1)).isTrue();
        assertThat(limiter.tryAcquire(1)).isFalse();
    }

    @io.github.poshjosh.ratelimiter.annotations.Rate(1)
    static class ResourceWithOnlyClassRate {
        void smile() { }
        static Method getMethodLimitedByClassRate() {
            try {
                return ResourceWithOnlyClassRate.class.getDeclaredMethod("smile");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void shouldUseClassRateGivenMethodOfResourceWithOnlyClassRate() {
        RateLimiter limiter = RateLimiterRegistry.getLimiter(
                ResourceWithOnlyClassRate.class,
                ResourceWithOnlyClassRate.getMethodLimitedByClassRate());
        assertThat(limiter).isNotNull();
        assertThat(limiter.tryAcquire(1)).isTrue();
        assertThat(limiter.tryAcquire(1)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(longs = {2_000, 100})
    void shouldNotBeAffectedByLongInitialDelay() throws InterruptedException {
        final long duration = 100;
        RateLimiterRegistry<String> RateLimiterRegistry = getRateLimiterRegistry(getRate(2, duration));
        Thread.sleep(duration + 1);
        assertTrue(RateLimiterRegistry.getRateLimiter(key).tryAcquire(), "Unable to acquire initial permit");
    }

    @ParameterizedTest
    @ValueSource(longs = {2_000, 100})
    void shouldExceedLimitAfterLongInitialDelay(long duration) throws InterruptedException {
        RateLimiterRegistry<String> RateLimiterRegistry = getRateLimiterRegistry(getRate(1, duration));
        Thread.sleep(duration + 10);
        assertTrue(RateLimiterRegistry.getRateLimiter(key).tryAcquire(), "Unable to acquire initial permit");
        assertFalse(RateLimiterRegistry.getRateLimiter(key).tryAcquire(), "Capable of acquiring additional permit");
    }

    @Test
    void veryLargeLimitShouldNotBeAffectedByDuration() {
        final long duration = 1;
        RateLimiterRegistry<String> RateLimiterRegistry = getRateLimiterRegistry(getRate(Long.MAX_VALUE, duration));
        for (int i = 0; i < 100; i++) {
            assertTrue(RateLimiterRegistry.getRateLimiter(key).tryAcquire(), "Unable to acquire permit " + i);
        }
    }

    @Test
    void immediateConsumeShouldSucceed() {
        RateLimiterRegistry<String> RateLimiterRegistry = perSecondRateLimiter(1);
        assertTrue(RateLimiterRegistry.getRateLimiter(key).tryAcquire(), "Unable to acquire initial permit");
    }

    @Test
    void testConsumeParameterValidation() {
        RateLimiterRegistry<String> RateLimiterRegistry = perSecondRateLimiter(999);
        assertThrowsRuntimeException(() -> RateLimiterRegistry.getRateLimiter(key).tryAcquire(-1));
        if (!supportsNullKeys) {
            assertThrowsRuntimeException(() -> RateLimiterRegistry.getRateLimiter((String)null).tryAcquire());
        }
    }

    protected <T> RateLimiterRegistry<T> perSecondRateLimiter(long amount) {
        return getRateLimiterRegistry(getRate(amount, durationMillis));
    }

    @Test
    void testNewInstanceParameterValidation() {
        assertThrowsRuntimeException(() -> getRateLimiterRegistry(getRate(-1, 1)));
        assertThrowsRuntimeException(() -> getRateLimiterRegistry(getRate(1, -1)));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 4})
    void shouldResetWhenLimitNotExceededWithinDuration(long limit) throws InterruptedException{
        final long duration = 2000;
        RateLimiterRegistry<String> RateLimiterRegistry = getRateLimiterRegistry(getRate(limit, duration));

        //long startMillis = 0;

        int i = 0;
        for (; i < limit; i++) {
            //System.out.println(i);
            //startMillis = System.currentTimeMillis();
            assertTrue(RateLimiterRegistry.getRateLimiter(key).tryAcquire(), "Unable to acquire permit " + i);
        }
        //System.out.println(i);
        assertFalse(RateLimiterRegistry.getRateLimiter(key).tryAcquire(), "Capable of acquiring permit " + limit);

        // Works but is a bit flaky
        //Thread.sleep(duration - (System.currentTimeMillis() - startMillis) + 1); // Leads to reset
        Thread.sleep(duration); // Leads to reset

        i = 0;
        for (; i < limit; i++) {
            //System.out.println(i);
            assertTrue(RateLimiterRegistry.getRateLimiter(key).tryAcquire(), "Unable to acquire permit " + i);
        }
        //System.out.println(i);
        assertFalse(RateLimiterRegistry.getRateLimiter(key).tryAcquire(), "Capable of acquiring permit " + limit);
    }

    @Test
    void shouldResetWhenAtThreshold() throws Exception{
        RateLimiterRegistry<String> RateLimiterRegistry = getRateLimiterRegistry(getRate(1, 0));
        RateLimiterRegistry.getRateLimiter(key).tryAcquire();

        // Simulate some time before the next recording
        // This way we can have a reset
        Thread.sleep(durationMillis + 500);

        RateLimiterRegistry.getRateLimiter(key).tryAcquire();
    }

    @Test
    void shouldFailWhenLimitExceeded() {
        RateLimiterRegistry<String> RateLimiterRegistry = getRateLimiterRegistry(getRate(2, 1000));
        assertThat(RateLimiterRegistry.getRateLimiter(key).tryAcquire()).isTrue();
        assertThat(RateLimiterRegistry.getRateLimiter(key).tryAcquire()).isTrue();
        assertThat(RateLimiterRegistry.getRateLimiter(key).tryAcquire()).isFalse();
    }

    static void assertTrue(boolean expression, String message) {
        assertThat(expression).withFailMessage(message).isTrue();
    }

    static void assertFalse(boolean expression, String message) {
        assertThat(expression).withFailMessage(message).isFalse();
    }

    static void assertThrowsRuntimeException(Executable executable) {
        assertThrows(RuntimeException.class, executable);
    }

    public <T> RateLimiterRegistry<T> getRateLimiterRegistry(Rate limit) {
        return RateLimiterRegistry.of(key, limit);
    }

    protected Rate getRate(long permits, long durationMillis) {
        return getRate(permits, durationMillis, factoryClass);
    }

    protected Rate getRate(long permits, long durationMillis, Class<? extends BandwidthFactory> factoryClass) {
        return Rate.of(permits, Duration.ofMillis(durationMillis), "", factoryClass);
    }
}
