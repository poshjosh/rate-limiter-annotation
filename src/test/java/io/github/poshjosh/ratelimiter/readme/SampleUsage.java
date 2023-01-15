package io.github.poshjosh.ratelimiter.readme;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.annotation.ResourceLimiterFactory;
import io.github.poshjosh.ratelimiter.annotations.Rate;

public class SampleUsage {

    static final int LIMIT = 3;

    static class RateLimitedResource {

        final ResourceLimiter resourceLimiter;

        RateLimitedResource(ResourceLimiter resourceLimiter) {
            this.resourceLimiter = resourceLimiter;
        }

        // Limited to 3 invocations every second
        @Rate(permits = LIMIT)
        void rateLimitedMethod() {

            if (!resourceLimiter.tryConsume("rateLimitedMethodId")) {
                throw new RuntimeException("Limit exceeded");
            }
        }
    }

    public static void main(String... args) {

        ResourceLimiter resourceLimiter = ResourceLimiterFactory.ofDefaults().create(RateLimitedResource.class);

        RateLimitedResource rateLimitedResource = new RateLimitedResource(resourceLimiter);

        int i = 0;
        for(; i < LIMIT; i++) {

            System.out.println("Invocation " + i + " of " + LIMIT);
            rateLimitedResource.rateLimitedMethod();
        }

        System.out.println("Invocation " + i + " of " + LIMIT + " should fail");
        // Should fail
        rateLimitedResource.rateLimitedMethod();
    }
}
