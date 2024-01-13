# rate limiter - annotation

__Distributed rate limiting simplified using annotations__

We believe that rate limiting should be as simple as:

```java
// All methods collectively limited to 10 permits per second
@Rate(10)
class RateLimitedResource {

    // 99 permits per second
    @Rate(99)
    public String smile() {
        return ":)";
    }

    // 2 permits per second only when system available memory is less than 1GB
    @Rate(permits = 2, condition = "jvm.memory.available<1GB") 
    public String greet(String who) {
        return "Hello " + who;
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
            <version>0.6.1</version> 
        </dependency>
```

### Concept 

The idea is to be able to rate limit multiple resources fluently and dynamically

```java
class DynamicRateLimiting {

    @Rate(name="resource-a", permits=1)
    static class ResourceA{}
    
    // Will be rate limited when system elapsed time is greater than 59 seconds
    @Rate(name="resource-b", permits=5, condition="sys.time.elapsed>PT59S")
    static class ResourceB{}

    public static void main(String... args) {

        RateLimiterFactory<String> rateLimiterFactory = RateLimiterFactory
                .of(ResourceA.class, ResourceB.class);

        rateLimiterFactory.getLimiter("resource-a").tryAcquire(); // true
        rateLimiterFactory.getLimiter("resource-a").tryAcquire(); // false

        rateLimiterFactory.getLimiter("resource-b").tryAcquire(); // false
    }
}
```


### Sample Usage

```java
import io.github.poshjosh.ratelimiter.RateLimiter;
import io.github.poshjosh.ratelimiter.RateLimiterFactory;
import io.github.poshjosh.ratelimiter.annotations.Rate;

public class SampleUsage {

    static class RateLimitedResource {

        RateLimiter rateLimiter = RateLimiterFactory
                .getLimiter(RateLimitedResource.class, "smile");

        // Limited to 3 invocations every second
        @Rate(name = "smile", permits = 3)
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
```

### Annotation Specification

Please read the [annotation specs](docs/ANNOTATION_SPECS.md). It is concise.

### Dependents

The following depend on this library:

- [rate-limiter-web-core](https://github.com/poshjosh/rate-limiter-web-core).

- [rate-limiter-spring](https://github.com/poshjosh/rate-limiter-spring).

- [rate-limiter-javaee](https://github.com/poshjosh/rate-limiter-javaee).
