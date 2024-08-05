# RateCondition Expression Language

A language for expressing the condition for rate limiting.

### Format

An expression is of format `LHS` `OPERATOR` `RHS` e.g `jvm.thread.count.started > 99`

`LHS` = `jvm.thread.count`,  `OPERATOR` = `>`,  `RHS` = `99`

| format                   | example                                              | description                                                            |  
|--------------------------|------------------------------------------------------|------------------------------------------------------------------------|
| LHS = RHS                | jvm.thread.count < 22                                | true, when the jvm.thread.count is less than 22                        |  
| LHS[key] = val           | sys.environment[limited] = true                      | true, when system environment named limited equals true                |  
| LHS = [A &#9122; B]      | jvm.current.thread.state = [BLOCKED &#9122; WAITING] | true, when the current thread state is either BLOCKED or WAITING       |
| LHS[key] = [A &#9122; B] | sys.environment[name] = [val_0 &#9122; val_1]        | true, when either val_0 or val_1 is value of system environment `name` |  
| LHS[key] = [A & B]       | sys.property[name] = [val_0 & val_1]                 | true, when both val_0 and val_1 are values of system property `name`   |  

Example:

```java
// 5 permits per second when available system memory is less than 1 GB
@Rate(permits = 5, when = "jvm.memory.available < 1GB")
class ResourceA{ }

class ResourceB{
    // 2 permits per second when available system memory is less than 1 GB, and user role is GUEST
    @Rate(permits = 2, when = "jvm.memory.available < 1GB & web.request.user.role = GUEST")
    void smile() {
        return ":)";
    }
}
```

### Supported Left Hand Side (LHS)

[View the full list of supported LHS here](https://github.com/poshjosh/rate-limiter/blob/master/docs/RATE-CONDITION-EXPRESSION-LANGUAGE.md)
