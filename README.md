# rate limiter - annotation

__Rate limiting simplified using annotations__

We believe rate limiting should as easy as:

```java
// All methods collectively limited to 10 permits per second
@Rate(10)
class RateLimitedResource {

    // 2 permits per second only when system free memory is less than 1GB
    @Rate(2) 
    @RateCondition("sys.memory.free<1GB")
    @Path("/greet")
    public String greet(String who) {
        return "Hello " + who;
    }

    // 50 permits per minute
    @Rate(permits = 50, duration =  TimeUnit.MINUTES)
    @Path("/smile")
    public String smile() {
        return ":)";
    }
}
```

Based on [rate-limiter](https://github.com/poshjosh/rate-limiter).

Please first read the [rate-limiter documentation](https://github.com/poshjosh/rate-limiter).

__For flexibility, this library offers a robust support for annotations.__

If the target is web applications, consider using any of:

- [rate-limiter-web-core](https://github.com/poshjosh/rate-limiter-web-core).

- [rate-limiter-spring](https://github.com/poshjosh/rate-limiter-spring).

- [rate-limiter-javaee](https://github.com/poshjosh/rate-limiter-javaee).

To add a dependency on `rate-limiter-annotation` using Maven, use the following:

```xml
        <dependency>
            <groupId>io.github.poshjosh</groupId>
            <artifactId>rate-limiter-annotation</artifactId>
            <version>0.2.0</version> 
        </dependency>
```

### Sample Usage

```java

import ResourceLimiterFactory;

public class SampleUsage {

    static final int permits = 3;

    static class RateLimitedResource {

        final ResourceLimiter resourceLimiter;

        RateLimitedResource(ResourceLimiter resourceLimiter) {
            this.resourceLimiter = resourceLimiter;
        }

        // Limited to 3 invocations every second
        void rateLimitedMethod() {

            if (!resourceLimiter.tryConsume("rateLimitedMethodId")) {
                throw new RuntimeException("Limit exceeded");
            }
        }
    }

    public static void main(String... args) {

        ResourceLimiter resourceLimiter = ResourceLimiterFactory.ofDefaults()
                .create(RateLimitedResource.class);

        RateLimitedResource rateLimitedResource = new RateLimitedResource(resourceLimiter);

        int i = 0;
        for (; i < LIMIT; i++) {

            System.out.println("Invocation " + i + " of " + LIMIT);
            rateLimitedResource.rateLimitedMethod();
        }

        System.out.println("Invocation " + i + " of " + LIMIT + " should fail");
        // Should fail
        rateLimitedResource.rateLimitedMethod();
    }
}
```

### Annotation Specification

Please read the [annotation specs](docs/ANNOTATION_SPECS.md). It is concise.

### Dependents

The following depend on this library:

- [rate-limiter-web-core](https://github.com/poshjosh/rate-limiter-web-core).

- [rate-limiter-spring](https://github.com/poshjosh/rate-limiter-spring).

- [rate-limiter-javaee](https://github.com/poshjosh/rate-limiter-javaee).
