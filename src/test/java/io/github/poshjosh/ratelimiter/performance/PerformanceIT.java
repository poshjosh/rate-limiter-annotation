package io.github.poshjosh.ratelimiter.performance;

import io.github.poshjosh.ratelimiter.RateLimiter;
import io.github.poshjosh.ratelimiter.RateLimiterFactory;
import io.github.poshjosh.ratelimiter.performance.dummyclasses.dummyclasses0.RateLimitedClass0;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.poshjosh.ratelimiter.performance.Helpers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

abstract class PerformanceIT {

    //
    // Do not use log level debug/trace, as tests may fail, due the overhead caused by logging
    //

    // At 10k iterations with 10 millis interval (100 req/sec)
//    @Test
//    void profileThis_resourceLimiting_withInterval_ShouldConsumeLimitedTimeAndMemory() throws InterruptedException{
//        resourceLimitingShouldConsumeLimitedTimeAndMemory(
//                RateLimitedClass0.METHOD_5_KEY, Usage.of(3500, 300_000), 10000, 10
//        );
//    }

    @Test
    void annotationProcessShouldConsumeLimitedTimeAndMemory() {

        final Usage usageBookmark = Usage.bookmark();

        givenRateLimiterFactory();

        final Usage recordedUsage = usageBookmark.current();

        // Generally -> duration = no. of classes x 4,   memory = no. of classes x 350k
        assertUsageLessOrEqualToLimit(
                "annotationProcessShouldConsumeLimitedTimeAndMemory()",
                recordedUsage, Usage.of(412, 36_050_000));
    }

    @Test
    void resourceLimiting_withInterval_ShouldConsumeLimitedTimeAndMemory() throws InterruptedException{
        garbageCollectAndWaitABit();
        resourceLimitingShouldConsumeLimitedTimeAndMemory(
                RateLimitedClass0.METHOD_5_KEY, Usage.of(350, 30_000), 100, 100
        );
    }

    @Test
    void resourceLimiting_withoutInterval_ShouldConsumeLimitedTimeAndMemory() throws InterruptedException{
        garbageCollectAndWaitABit();
        resourceLimitingShouldConsumeLimitedTimeAndMemory(
                RateLimitedClass0.METHOD_5_KEY, Usage.of(50, 3_000_000), 10_000, 0
        );
    }

    @Test
    void firstCallToGet_shouldConsumeLimitedTimeAndMemory() {
        final RateLimiterFactory<String> rateLimiterFactory = givenRateLimiterFactory(annotatedClasses());
        final Usage bookmark = Usage.bookmark();
        rateLimiterFactory.getRateLimiter(RateLimitedClass0.METHOD_5_KEY);
        final Usage recordedUsage = bookmark.current();
        assertUsageLessOrEqualToLimit(
                "firstCallToGet_shouldConsumeLimitedTimeAndMemory()",
                recordedUsage, Usage.of(30, 30_000));
    }

    @Test
    void secondCallToGet_shouldConsumeLimitedTimeAndMemory() {
        final RateLimiterFactory<String> rateLimiterFactory = givenRateLimiterFactory();
        rateLimiterFactory.getRateLimiter(RateLimitedClass0.METHOD_5_KEY);
        final Usage bookmark = Usage.bookmark();
        rateLimiterFactory.getRateLimiter(RateLimitedClass0.METHOD_5_KEY);
        final Usage recordedUsage = bookmark.current();
        assertUsageLessOrEqualToLimit(
                "secondCallToGet_shouldConsumeLimitedTimeAndMemory()",
                recordedUsage, Usage.of(3, 3_00));
    }

    @Test
    void get_shouldConsumeLimitedTimeAndMemory() throws InterruptedException {
        garbageCollectAndWaitABit();
        final RateLimiterFactory<Object> rateLimiterFactory = givenRateLimiterFactory();
        final List<Method> methods = annotatedClassMethods();
        final int count = methods.size();
        final Usage bookmark = Usage.bookmark();
        for(int i = 0; i < count; i++) {
            final Method method = methods.get(i);
            rateLimiterFactory.getRateLimiter(method);
        }
        final Usage recordedUsage = bookmark.current();
        assertUsageLessOrEqualToLimit(
                "get_shouldConsumeLimitedTimeAndMemory()",
                recordedUsage, Usage.of(count/10, 50_000 * count));
    }

    @Test
    void tryConsume_shouldConsumeLimitedTimeAndMemory() {

        final RateLimiter rateLimiter = givenRateLimiterFactory()
                .getRateLimiter(RateLimitedClass0.METHOD_5_KEY);

        final Usage usageBookmark = Usage.bookmark();

        rateLimiter.tryAcquire();

        final Usage recordedUsage = usageBookmark.current();

        assertUsageLessOrEqualToLimit(
                "tryConsume_shouldConsumeLimitedTimeAndMemory()",
                recordedUsage, Usage.of(20, 200));
    }

    private void resourceLimitingShouldConsumeLimitedTimeAndMemory(
            String rateId, Usage usageLimit, int iterations, int intervalMillis)
            throws InterruptedException{
        final Map<String, Object> args = new LinkedHashMap<>();
        args.put("rateId", rateId);
        args.put("usageLimit", usageLimit);
        args.put("iterations", iterations);
        args.put("intervalMillis", intervalMillis);
        final String method = "resourceLimitingShouldConsumeLimitedTimeAndMemory(" + args + ")";

        final RateLimiterFactory<String> rateLimiterFactory = givenRateLimiterFactory();

        final Usage usageBookmark = Usage.bookmark();

        int successCount = 0;
        for (int i = 0; i < iterations; i++) {
            if(rateLimiterFactory.getRateLimiter(rateId).tryAcquire(1)) {
                ++successCount;
            }
            waitFor(intervalMillis);
        }

        final int totalIntervalMillis = iterations * intervalMillis;
        Usage _curr = usageBookmark.current();
        final Usage recordedUsage = Usage.of(_curr.getDuration() - totalIntervalMillis, _curr.getMemory());

        System.out.println("Rate limited: " + successCount + " of " + iterations);
        assertUsageLessOrEqualToLimit(method, recordedUsage, usageLimit);
    }

    private void assertUsageLessOrEqualToLimit(String key, Usage recordedUsage, Usage usageLimit) {
        System.out.println("\n" + key);
        System.out.println("   Recorded " + recordedUsage);
        System.out.println("Max Allowed " + usageLimit);
        assertFalse(recordedUsage.isAnyUsageGreaterThan(usageLimit),
                "Usage should be less or equal to limit, but was not.\nUsage: " +
                        recordedUsage + "\nLimit: " + usageLimit);
    }

    private void garbageCollectAndWaitABit() throws InterruptedException {
        Runtime.getRuntime().gc();
        waitFor(3000);
    }

    private void waitFor(long timeoutMillis) throws InterruptedException{
        if (timeoutMillis < 10) {
            return;
        }
        try {
            Thread.sleep(timeoutMillis);
        }catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}
