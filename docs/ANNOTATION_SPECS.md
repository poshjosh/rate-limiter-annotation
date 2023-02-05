# Annotation Specification

- `@Rate` annotations could be placed on a class, super class or their respective methods.

- `@Rate` annotations at the class level applies to all methods of the class.
  
- The same rate could be defined for multiple targets as follows:

```java
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;

// 5 permits per second for users in role GUEST
@Rate(5)  
@RateCondition("web.request.user.role=GUEST")
@interface MyRateGroup { }
``` 
You could then use the above annotation for as many classes/methods that apply.

- Use a `@RateGroup` annotation for customizing a group of co-located `@Rate` annotations. 

- Each `@RateGroup` is identified by name.

- `@RateGroup` operator may not be specified when multiple rate-conditions are specified.
  
**Simple Example**

All the members of the rate limit group below will have the same rate applied.
All the members are bound by the group name.

```java
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;

// 1 permit per second when system memory is below 1GB
@Rate(1) 
@RateCondition("sys.memory.free<1G")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface GuestUserRate { }

@GuestUserRate 
static class MyRateGroupMember0 {
    void method0() { }
}

static class MyRateGroupMember1 {

    @GuestUserRate 
    void method0() { }
}
```

**Complex Example**

Given the following rate groups:

```java
// 1 request per second for requests whose locale is not either en_US or en_UK
@Rate(1)
@RateCondition("web.request.locale!=[en_US|en_UK]")
@RateGroup("class-group")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@interface ClassGroup{ }

// 1 request per second for requests with the specified header
@Rate(1)
@RateCondition("web.request.header=X-Rate-Limited")
@RateGroup("method-group")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@interface MethodGroup{ }
```

And 3 resource classes `Resource1`, `Resource2` and `Resource3`, rate limited as shown below:

```java
class Resource1{
    
    @Rate(1)
    void methodA() {}

    @Rate(1)
    void methodB() {}

    @Rate(1)
    @MethodGroup
    void methodC() {}
}
```

```java
@ClassGroup
class Resource2{
    
    @Rate(1)
    void methodA() {}

    @Rate(1)
    @MethodGroup
    void methodB() {}

    @Rate(1)
    void methodC() {}
}
```

```java
@ClassGroup
class Resource3{
    
    @Rate(1)
    void methodA() {}
}
```

**Resulting Hierarchy**

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