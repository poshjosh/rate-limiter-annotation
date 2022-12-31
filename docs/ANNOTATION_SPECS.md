# Annotation Specification

- The `@RateLimit` annotation may be placed on a super class.

- A `@RateLimit` annotation at the class level applies to all methods of the class having a
  `@RateLimit` annotation.
  
- Multiple annotations on an element (e.g class, method) many not belong to different groups.

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