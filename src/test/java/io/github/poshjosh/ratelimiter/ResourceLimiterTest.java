package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.util.Rate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceLimiterTest {

    final String key = "0";

    final int durationMillis = 2000;

    private final Class<? extends BandwidthFactory> factoryClass = BandwidthFactory.AllOrNothing.class;
    private final boolean supportsNullKeys = true;

    @ParameterizedTest
    @ValueSource(longs = {2_000, 100})
    void shouldNotBeAffectedByLongInitialDelay() throws InterruptedException {
        final long duration = 100;
        ResourceLimiter<String> resourceLimiter = getRateLimiter(getRate(2, duration));
        Thread.sleep(duration + 1);
        assertTrue(resourceLimiter.tryConsume(key), "Unable to acquire initial permit");
    }

    @ParameterizedTest
    @ValueSource(longs = {2_000, 100})
    void shouldExceedLimitAfterLongInitialDelay(long duration) throws InterruptedException {
        ResourceLimiter<String> resourceLimiter = getRateLimiter(getRate(1, duration));
        Thread.sleep(duration + 10);
        assertTrue(resourceLimiter.tryConsume(key), "Unable to acquire initial permit");
        assertFalse(resourceLimiter.tryConsume(key), "Capable of acquiring additional permit");
    }

    @Test
    void veryLargeLimitShouldNotBeAffectedByDuration() {
        final long duration = 1;
        ResourceLimiter<String> resourceLimiter = getRateLimiter(getRate(Long.MAX_VALUE, duration));
        for (int i = 0; i < 100; i++) {
            assertTrue(resourceLimiter.tryConsume(key), "Unable to acquire permit " + i);
        }
    }

    @Test
    void immediateConsumeShouldSucceed() {
        ResourceLimiter<String> resourceLimiter = perSecondRateLimiter(1);
        assertTrue(resourceLimiter.tryConsume(key), "Unable to acquire initial permit");
    }

    @Test
    void testConsumeParameterValidation() {
        ResourceLimiter<String> resourceLimiter = perSecondRateLimiter(999);
        assertThrowsRuntimeException(() -> resourceLimiter.tryConsume(key, -1));
        if (!supportsNullKeys) {
            assertThrowsRuntimeException(() -> resourceLimiter.tryConsume(null, 1));
        }
    }

    protected <T> ResourceLimiter<T> perSecondRateLimiter(long amount) {
        return getRateLimiter(getRate(amount, durationMillis));
    }

    @Test
    void testNewInstanceParameterValidation() {
        assertThrowsRuntimeException(() -> getRateLimiter(getRate(-1, 1)));
        assertThrowsRuntimeException(() -> getRateLimiter(getRate(1, -1)));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 4})
    void shouldResetWhenLimitNotExceededWithinDuration(long limit) throws InterruptedException{
        final long duration = 2000;
        ResourceLimiter<String> resourceLimiter = getRateLimiter(getRate(limit, duration));

        //long startMillis = 0;

        int i = 0;
        for (; i < limit; i++) {
            //System.out.println(i);
            //startMillis = System.currentTimeMillis();
            assertTrue(resourceLimiter.tryConsume(key), "Unable to acquire permit " + i);
        }
        //System.out.println(i);
        assertFalse(resourceLimiter.tryConsume(key), "Capable of acquiring permit " + limit);

        // Works but is a bit flaky
        //Thread.sleep(duration - (System.currentTimeMillis() - startMillis) + 1); // Leads to reset
        Thread.sleep(duration); // Leads to reset

        i = 0;
        for (; i < limit; i++) {
            //System.out.println(i);
            assertTrue(resourceLimiter.tryConsume(key), "Unable to acquire permit " + i);
        }
        //System.out.println(i);
        assertFalse(resourceLimiter.tryConsume(key), "Capable of acquiring permit " + limit);
    }

    @Test
    void shouldResetWhenAtThreshold() throws Exception{
        ResourceLimiter<String> resourceLimiter = getRateLimiter(getRate(1, 0));
        resourceLimiter.tryConsume(key);

        // Simulate some time before the next recording
        // This way we can have a reset
        Thread.sleep(durationMillis + 500);

        resourceLimiter.tryConsume(key);
    }

    @Test
    void shouldFailWhenLimitExceeded() {
        ResourceLimiter<String> resourceLimiter = getRateLimiter(getRate(2, 1000));
        assertThat(resourceLimiter.tryConsume(key)).isTrue();
        assertThat(resourceLimiter.tryConsume(key)).isTrue();
        assertThat(resourceLimiter.tryConsume(key)).isFalse();
    }

    @Test
    void testAndThen() {
        ResourceLimiter<String> a = getRateLimiter(
                getRate(10, 1000, BandwidthFactory.AllOrNothing.class));
        ResourceLimiter<String> b = getRateLimiter(
                getRate(1, 1000, BandwidthFactory.AllOrNothing.class));
        ResourceLimiter<String> c = a.andThen(b);
        assertTrue(c.tryConsume(key), "Unable to acquire initial permit");
        assertFalse(c.tryConsume(key), "Capable of acquiring additional permit");
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

    public <T> ResourceLimiter<T> getRateLimiter(Rate... limits) {
        return ResourceLimiter.of(key, limits);
    }

    protected Rate getRate(long permits, long durationMillis) {
        return getRate(permits, durationMillis, factoryClass);
    }

    protected Rate getRate(long permits, long durationMillis, Class<? extends BandwidthFactory> factoryClass) {
        return Rate.of(permits, Duration.ofMillis(durationMillis), "", factoryClass);
    }
}
