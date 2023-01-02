package com.looseboxes.ratelimiter.readme;

import com.looseboxes.ratelimiter.ResourceLimiter;
import com.looseboxes.ratelimiter.annotation.ResourceLimiterFromAnnotationFactory;
import com.looseboxes.ratelimiter.annotations.RateLimit;

import java.util.concurrent.TimeUnit;

public class SampleUsage {

    static final int LIMIT = 3;

    static class RateLimitedResource {

        final ResourceLimiter resourceLimiter;

        RateLimitedResource(ResourceLimiter resourceLimiter) {
            this.resourceLimiter = resourceLimiter;
        }

        // Limited to 3 invocations every second
        @RateLimit(permits = LIMIT, duration = 1)
        void rateLimitedMethod() {

            if (!resourceLimiter.tryConsume("rateLimitedMethodId")) {
                throw new RuntimeException("Limit exceeded");
            }
        }
    }

    public static void main(String... args) {

        ResourceLimiter resourceLimiter = ResourceLimiterFromAnnotationFactory.ofDefaults().create(RateLimitedResource.class);

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
