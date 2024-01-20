package io.github.poshjosh.ratelimiter.readme;

import io.github.poshjosh.ratelimiter.RateLimiter;
import io.github.poshjosh.ratelimiter.RateLimiterFactory;
import io.github.poshjosh.ratelimiter.annotations.Rate;

public class SampleUsage {

    static class RateLimitedResource {

        RateLimiter rateLimiter = RateLimiterFactory
                .getLimiter(RateLimitedResource.class, "smile");

        // Limited to 3 invocations every second
        @Rate(id = "smile", permits = 3)
        String smile() {
            if (!rateLimiter.tryAcquire()) {
                throw new RuntimeException("Limit exceeded");
            }
            return ":)";
        }
    }

    public static void main(String... args) {

        RateLimitedResource rateLimitedResource = new RateLimitedResource();

        int i = 0;
        for(; i < 3; i++) {

            System.out.println("Invocation " + i + " of 3 should succeed");
            rateLimitedResource.smile();
        }

        System.out.println("Invocation " + i + " of 3 should fail");
        rateLimitedResource.smile();
    }
}
