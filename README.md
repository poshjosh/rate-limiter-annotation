# rate limiter - annotations

__Rate limiting simplified using annotations__

Based on [rate-limiter](https://github.com/poshjosh/rate-limiter).

Please first read the [rate-limiter documentation](https://github.com/poshjosh/rate-limiter).

For flexibility, this library offers a robust support for annotations.

```java
// All methods collectively limited to 120 invocations every 1 minute
@RateLimit(limit = 120, duration = 1, timeUnit = TimeUnit.MINUTES)
class RateLimitedResource {

    // Method limited to 3 invocations every 2 seconds OR 100 invocations every 1 minute
    @RateLimit(limit = 3, duration = 2000)
    @RateLimit(limit = 100, duration = 1, timeUnit = TimeUnit.MINUTES)
    void rateLimitedMethod_1() {
        return "Hello World 1!";
    }

    // Method limited to 3 invocations every 1 second
    @RateLimit(limit = 3, duration = 1000)
    void rateLimitedMethod_2() {
        return "Hello World 2!";
    }
}
```

### Sample Usage

```java

import com.looseboxes.ratelimiter.annotation.ResourceLimiterFromAnnotationFactory;

public class SampleUsage {

  static final int LIMIT = 3;

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

    ResourceLimiter resourceLimiter = ResourceLimiterFromAnnotationFactory.of().create(RateLimitedResource.class);

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
