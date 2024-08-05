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
    @Rate(permits = 2, condition = "jvm.memory.available < 1GB") 
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
            <version>0.8.0</version> 
        </dependency>
```

### Concept 

The idea is to be able to rate limit multiple resources fluently and dynamically

```java
class DynamicRateLimiting {

    @Rate(id="resource-a", permits=1)
    static class ResourceA{}
    
    // Will be rate limited when system elapsed time is greater than 59 seconds
    @Rate(id="resource-b", permits=5, condition="sys.time.elapsed > PT59S")
    static class ResourceB{}

    public static void main(String... args) {

        RateLimiterRegistry<String> rateLimiterRegistry = RateLimiterRegistry
                .of(ResourceA.class, ResourceB.class);

        rateLimiterRegistry.getLimiter("resource-a").tryAcquire(); // true
        rateLimiterRegistry.getLimiter("resource-a").tryAcquire(); // false

        rateLimiterRegistry.getLimiter("resource-b").tryAcquire(); // false
    }
}
```


### Sample Usage

```java
import io.github.poshjosh.ratelimiter.RateLimiter;
import io.github.poshjosh.ratelimiter.RateLimiterRegistries;
import io.github.poshjosh.ratelimiter.annotations.Rate;

public class SampleUsage {

    static class RateLimitedResource {

        RateLimiter rateLimiter = RateLimiterRegistries.getLimiter(RateLimitedResource.class,
                "smile");

        // Limited to 3 invocations every second
        @Rate(id = "smile", permits = 3) String smile() {
            if (!rateLimiter.tryAcquire()) {
                throw new RuntimeException("Limit exceeded");
            }
            return ":)";
        }
    }

    public static void main(String... args) {

        RateLimitedResource rateLimitedResource = new RateLimitedResource();

        int i = 0;
        for (; i < 3; i++) {

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

### Bandwidth store

You could use a distributed cache to store Bandwidths. First implement
`BandwidthStore`. The example implementation below uses `spring-boot-starter-data-redis`

```java
import org.springframework.data.redis.core.RedisTemplate;

public class RedisBandwidthStore implements BandwidthsStore<String> {
    private final RedisTemplate<String, Bandwidth> redisTemplate;
    
    public RedisBandwidthStore(RedisTemplate<String, Bandwidth> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override 
    public Bandwidth get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    @Override 
    public void put(String key, Bandwidth bandwidth) {
        redisTemplate.opsForValue().set(key, bandwidth);
    }
}
```

Then use the `BandwidthStore` as shown below:

```java
public class WithCustomBandwidthStore {
    
    public RateLimiter getRateLimiter(BandwidthsStore store) {
        RateLimiterContext context = RateLimiterContext.builder()
                .classes(MyRateLimitedClass.class)
                .store(store)
                .build();
        return RateLimiterRegistry.of(context).getRateLimiter("ID");
    }
}
```

### Dependents

The following depend on this library:

- [rate-limiter-web-core](https://github.com/poshjosh/rate-limiter-web-core).

- [rate-limiter-spring](https://github.com/poshjosh/rate-limiter-spring).

- [rate-limiter-javaee](https://github.com/poshjosh/rate-limiter-javaee).
