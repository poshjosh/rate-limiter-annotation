# RateCondition Expression Language

A language for expressing the condition for rate limiting.

An expression is of format `LHS` `OPERATOR` `RHS` e.g `jvm.thread.count>99`

`LHS` = `jvm.thread.count`,
`OPERATOR` = `>`,
`RHS` = `99`

Examples:

```java
// 5 permits per second when available system memory is less than 1 giga byte
@Rate(5)
@RateCondition("sys.memory.available<1GB")
class Resource{ }
```

### jvm.thread

| name                                  | description |
|---------------------------------------|-------------|
| `jvm.thread.count`                    |             |
| `jvm.thread.count.daemon`             |             |  
| `jvm.thread.count.deadlocked`         |             |
| `jvm.thread.count.deadlocked.monitor` |             |
| `jvm.thread.count.peak`               |             |
| `jvm.thread.count.started`            |             |
| `jvm.thread.current.count.blocked`    |             |
|  `jvm.thread.current.count.waited`    |             |

__jvm.thread(.current).count__ supported format `digits` e.g `128`, `9`

| name                           | description                                            |
|--------------------------------|--------------------------------------------------------|
| `jvm.thread.current.state`     | `java.lang.Thread.State` of the current thread.        |
| `jvm.thread.current.suspended` | if the current thread is suspended                     |

__jvm.thread.current.state__ supported RHS values [NEW | RUNNABLE |BLOCKED | WAITING | TIMED_WAITING | TERMINATED]

__jvm.thread.current.suspended__ supported RHS values [true | false]

| name                              | description |
|-----------------------------------|-------------|
| `jvm.thread.current.time.blocked` |             |
| `jvm.thread.current.time.cpu`     |             |
| `jvm.thread.current.time.user`    |             |
| `jvm.thread.current.time.waited`  |             |

__jvm.thread.current.time__ supported input formats: ISO-8601 duration format `PnDTnHnMn.nS` with days
considered to be exactly 24 hours. See `java.time.Duration#parse(CharSequence)` for some
examples of this format

### sys.environment

| name              | description                                                    |  
|-------------------|----------------------------------------------------------------|
| `sys.environment` | Use environment key-value pairs e.g `sys.environment={LANG=C}` |


### sys.memory

Support must be provided for the expression. Support is provided by default for the following:

| name                   | description                                                                      |
|------------------------|----------------------------------------------------------------------------------|
| `sys.memory.available` | (_available memory in the JVM i.e. Maximum heap size (`Xmx`) minus used memory_) | 
| `sys.memory.free`      | (_amount of free memory in the JVM_)                                             |                                     
| `sys.memory.max`       | (_max amount of memory that the JVM will attempt to use_)                        |                      
| `sys.memory.total`     | (_total amount of memory in the JVM_)                                            |
|  `sys.memory.used`     | (_total minus free memory_)                                                      |

__sys.memory__ supported input formats: `digits`, `digits[B|KB|MB|GB|TB|PB|EB|ZB|YB]` 
e.g `1000000`, `1_000_000`, `1GB`, `1gb`

### sys.property

| name           | description                                                        |  
|----------------|--------------------------------------------------------------------|
| `sys.property` | Use property key-value pairs e.g `sys.property={user.name!=guest}` |

### sys.time

| name                | description                              |
|---------------------|------------------------------------------|
| `sys.time`          | (_local date time_)                      |
|  `sys.time.elapsed` | (_time elapsed since application start_) |

__sys.time__ supported input formats: ISO-8601 Time formats:
`uuuu-MM-dd'T'HH:mm`
`uuuu-MM-dd'T'HH:mm:ss`
`uuuu-MM-dd'T'HH:mm:ss.SSS`
`uuuu-MM-dd'T'HH:mm:ss.SSSSSS`
`uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS`

__sys.time.elapsed__ supported input formats: ISO-8601 duration format `PnDTnHnMn.nS` with days 
considered to be exactly 24 hours. See `java.time.Duration#parse(CharSequence)` for some 
examples of this format.

### Operators

`=`  EQUALS

`>`  GREATER

`>=` GREATER_OR_EQUALS

`<`  LESS

`<=` LESS_OR_EQUALS

`%`  LIKE

`^`  STARTS_WITH

`$`  ENDS_WITH

`!`  NOT (Negates other operators e.g `!=` or `!%`)




 
