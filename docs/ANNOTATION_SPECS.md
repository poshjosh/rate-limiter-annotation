# Annotation Specification

- The `@Rate` annotation may be placed on a super class.

- The `@Rate` annotation at the class level applies to all methods of the class having a
  `@Rate` annotation.
  
- A `@Rate` annotation may be assigned to a group using a `@RateGroup` annotation.

- Each `@RateGroup` is identified by name.
  
- Each `@RateGroup` may be initialized (i.e co-located with `@Rate`s) at only one location.
  Apart from that one location, the `@RateGroup` may be placed at other locations, as long
  as it is not co-located with `@Rate`.

- If a `@RateGroup` annotation is not specified the `@Rate` annotation, is
  assigned to a default group:

    * At the class level, the group is named after the fully qualified class name.

    * At the method level, the group is named after the fully qualified class name and method signature.
  
**Simple Example**

All the members of the rate limit group below will have the same rate applied.
All the members are bound by the group name.

```java
@Rate(1)
@RateGroup(MY_RATE_GROUP_NAME)
public @interface MyRateGroup { }

@RateGroup(MY_RATE_GROUP_NAME)
static class MyRateGroupMember0{
    
    void method0() {}
}

static class MyRateGroupMember1{
    
    @RateGroup(MY_RATE_GROUP_NAME)
    void method0() {}
}
```

**Complex Example**

Lets say we have 3 classes `Resource1`, `Resource2` and `Resource3`; rate limited as shown below:

```java
class Resource1{
    
    @Rate(permits = 1)
    void methodA() {}

    @Rate(permits = 1)
    void methodB() {}

    @Rate(permits = 1)
    @RateGroup("method-group")
    void methodC() {}
}
```

```java
@RateGroup("class-group")
class Resource2{
    
    @Rate(permits = 1)
    void methodA() {}

    @Rate(permits = 1)
    @RateGroup("method-group")
    void methodB() {}

    @Rate(permits = 1)
    void methodC() {}
}
```

```java
@RateGroup("class-group")
class Resource3{
    
    @Rate(permits = 1)
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