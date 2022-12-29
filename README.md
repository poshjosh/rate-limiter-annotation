# rate limiter - annotations

__Rate limiting simplified using annotations__

Based on [rate-limiter](https://github.com/poshjosh/rate-limiter).

Please first read the [rate-limiter documentation](https://github.com/poshjosh/rate-limiter).


```java
class RateLimitedResource {

    // Method limited to 3 invocations every 2 seconds
    @RateLimit(limit = 3, duration = 2000)
    String rateLimitedMethod() {
        return "Hello World!";
    }
}
```

For flexibility, a robust support for annotations.

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

- The `@RateLimit` annotation may be placed on a super class.

- A `@RateLimit` annotation at the class level applies to all methods of the class having a
  `@RateLimit` annotation.

- A `@RateLimit` annotation may be assigned to a group using a `@RateLimitGroup` annotation.

- If A `@RateLimitGroup` annotation is not specified the `@RateLimit` annotation, is
  assigned to a default group:

    * At the class level, the group is named after the fully qualified class name.

    * At the method level, the group is named after the fully qualified class name and method signature.

- The `@RateLimitGroup` annotation may span multiple classes or methods but not both.

**Example**

Lets say we have 3 classes `Resource1`, `Resource2` and `Resource3`; rate limited as shown below:

```java
class Resource1{
    
    @RateLimit(limit = 1, duration = 999)
    void methodA() {}

    @RateLimit(limit = 1, duration = 999)
    void methodB() {}

    @RateLimit(limit = 1, duration = 999)
    @RateLimitGroup("method-group")
    void methodC() {}
}
```

```java
@RateLimitGroup("class-group")
class Resource2{
    
    @RateLimit(limit = 1, duration = 999)
    void methodA() {}

    @RateLimit(limit = 1, duration = 999)
    @RateLimitGroup("method-group")
    void methodB() {}

    @RateLimit(limit = 1, duration = 999)
    void methodC() {}
}
```

```java
@RateLimitGroup("class-group")
class Resource3{
    
    @RateLimit(limit = 1, duration = 999)
    void methodA() {}
}
```

**Example Hierarchy**

```
                                              root
                                               |
              -------------------------------------------------------------------
              |                                |                                |    
         class-group                      method-group                          |       
              |                                |                                |                
    ---------------------                      |                                |
    |                   |                      |                                |
Resource2           Resource3                  |                            Resource1
    |                   |                      |                                | 
Resource2#methodA   Resource3#methodA   Resource1#methodC                   Resource1#methodA
Resource2#methodC                       Resource2#methodB                   Resource1#methodB

```

### Dependents

The following depend on this library:

- [rate-limiter-web-core](https://github.com/poshjosh/rate-limiter-web-core).

- [rate-limiter-spring](https://github.com/poshjosh/rate-limiter-spring).

- [rate-limiter-javaee](https://github.com/poshjosh/rate-limiter-javaee).
