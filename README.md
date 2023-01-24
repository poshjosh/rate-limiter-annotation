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
            <version>0.3.1</version> 
        </dependency>
```

### Sample Usage

```java
import io.github.poshjosh.ratelimiter.ResourceLimiter;
import io.github.poshjosh.ratelimiter.ResourceLimiters;
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

        ResourceLimiter<String> resourceLimiter = ResourceLimiters.of(RateLimitedResource.class);

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
```

### Annotation Specification

Please read the [annotation specs](docs/ANNOTATION_SPECS.md). It is concise.

### Dependents

The following depend on this library:

- [rate-limiter-web-core](https://github.com/poshjosh/rate-limiter-web-core).

- [rate-limiter-spring](https://github.com/poshjosh/rate-limiter-spring).

- [rate-limiter-javaee](https://github.com/poshjosh/rate-limiter-javaee).
