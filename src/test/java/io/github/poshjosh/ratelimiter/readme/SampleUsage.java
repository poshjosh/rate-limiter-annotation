package io.github.poshjosh.ratelimiter.readme;

import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.annotations.Rate;

public class SampleUsage {

    static class RateLimitedResource {

        final ResourceLimiter<String> resourceLimiter;

        RateLimitedResource(ResourceLimiter<String> resourceLimiter) {
            this.resourceLimiter = resourceLimiter;
        }

        // Limited to 3 invocations every second
        @Rate(name = "smile", permits = 3)
        String smile() {
            if (!resourceLimiter.tryConsume("smile")) {
                throw new RuntimeException("Limit exceeded");
            }
            return ":)";
        }
    }

    public static void main(String... args) {

        ResourceLimiter<String> resourceLimiter = ResourceLimiter.of(RateLimitedResource.class);

        RateLimitedResource rateLimitedResource = new RateLimitedResource(resourceLimiter);

        int i = 0;
        for(; i < 3; i++) {

            System.out.println("Invocation " + i + " of 3 should succeed");
            rateLimitedResource.smile();
        }

        System.out.println("Invocation " + i + " of 3 should fail");
        rateLimitedResource.smile();
    }
}
