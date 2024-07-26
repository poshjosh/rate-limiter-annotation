# RateCondition Expression Language

A language for expressing the condition for rate limiting.

### Format

An expression is of format `LHS` `OPERATOR` `RHS` e.g `jvm.thread.count.started>99`

`LHS` = `jvm.thread.count`,  `OPERATOR` = `>`,  `RHS` = `99`

| format                | example                                        | description                                             |  
|-----------------------|------------------------------------------------|---------------------------------------------------------|
| LHS=RHS               | web.request.header=X-RateLimit-Limit           | true, when the X-RateLimit-Limit header exists          |  
| LHS={key=val}         | web.request.parameter={limited=true}           | true, when request parameter limited equals true        |  
| LHS=[A&#9122;B]       | web.request.user.role=[GUEST&#9122;RESTRICTED] | true, when the user role is either GUEST or RESTRICTED  |
| LHS={key=[A&#9122;B]} | web.request.header={name=[val_0&#9122;val_1]}  | true, when either val_0 or val_1 is set a header        |  
| LHS={key=[A&B]}       | web.request.header={name=[val_0&val_1]}        | true, when both val_0 and val_1 are set as headers      |  

Example:

```java
// 5 permits per second when available system memory is less than 1 GB
@Rate(permits = 5, when = "jvm.memory.available<1G")
class ResourceA{ }

class ResourceB{
    // 2 permits per second when available system memory is less than 1 GB, and user role is GUEST
    @Rate(permits = 2, when = "jvm.memory.available<1G & web.request.user.role=GUEST")
    void smile() {
        return ":)";
    }
}
```

### Supported Left Hand Side (LHS)

[View the full list of supported LHS here](https://github.com/poshjosh/rate-limiter/blob/master/docs/RATE-CONDITION-EXPRESSION-LANGUAGE.md)
