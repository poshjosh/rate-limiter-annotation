package io.github.poshjosh.ratelimiter.performance;

import io.github.poshjosh.ratelimiter.RateLimiterFactory;
import io.github.poshjosh.ratelimiter.performance.dummyclasses.dummyclasses0.RateLimitedClass0;
import org.junit.jupiter.api.Test;

import static io.github.poshjosh.ratelimiter.performance.Helpers.annotatedClasses;
import static io.github.poshjosh.ratelimiter.performance.Helpers.givenRateLimiterFactory;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AnnotationProcessingPerformanceIT {

    //
    // Do not use log level debug/trace, as tests may fail, due the overhead caused by logging
    //

    @Test
    void annotationProcessShouldConsumeLimitedTimeAndMemory() {

        // Generally -> duration = no. of classes x 4,   memory = no. of classes x 350k
        final Usage usageLimit = Usage.of(412, 36_050_000);

        final Usage usageBookmark = Usage.bookmark();

        givenRateLimiterFactory(annotatedClasses());

        final Usage recordedUsage = usageBookmark.current();

        System.out.println("   Recorded " + recordedUsage);
        System.out.println("Max Allowed " + usageLimit);

        assertFalse(recordedUsage.isAnyUsageGreaterThan(usageLimit),
                "Usage should be less or equal to limit, but was not.\nUsage: " +
                        recordedUsage + "\nLimit: " + usageLimit);
    }

    @Test
    void resourceLimiting_withInterval_ShouldConsumeLimitedTimeAndMemory() throws InterruptedException{
        resourceLimitingShouldConsumeLimitedTimeAndMemory(
                RateLimitedClass0.METHOD_5_KEY, Usage.of(350, 0), 100, 100
        );
    }

    @Test
    void resourceLimiting_withoutInterval_ShouldConsumeLimitedTimeAndMemory() throws InterruptedException{
        resourceLimitingShouldConsumeLimitedTimeAndMemory(
                RateLimitedClass0.METHOD_5_KEY, Usage.of(50, 3_000_000), 10_000, 0
        );
    }

    private void resourceLimitingShouldConsumeLimitedTimeAndMemory(
            String key, Usage usageLimit, int iterations, int intervalMillis)
            throws InterruptedException{

        final RateLimiterFactory<String> rateLimiterFactory = givenRateLimiterFactory(annotatedClasses());

        final Usage usageBookmark = Usage.bookmark();

        int successCount = 0;
        for (int i = 0; i < iterations; i++) {
            if(rateLimiterFactory.getRateLimiter(key).tryAcquire(1)) {
                ++successCount;
            }
            waitFor(intervalMillis);
        }

        final int totalIntervalMillis = iterations * intervalMillis;
        Usage _curr = usageBookmark.current();
        final Usage recordedUsage = Usage.of(_curr.getDuration() - totalIntervalMillis, _curr.getMemory());

        System.out.println("   Recorded " + recordedUsage + ", rate limited: " + successCount + " of " + iterations);
        System.out.println("Max Allowed " + usageLimit);

        assertFalse(recordedUsage.isAnyUsageGreaterThan(usageLimit),
                "Usage should be less or equal to limit, but was not.\nUsage: " +
                        recordedUsage + "\nLimit: " + usageLimit);
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
