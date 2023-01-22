# RateCondition Expression Language

A language for expressing the condition for rate limiting.

Examples:

```java
@Rate(5)
@RateCondition("sys.memory.available<1GB")
class Resource{ }
```

### sys.memory

Support must be provided for the expression. Support is provided by default for the following:

name                   | description
-----------------------|------------
`sys.memory.available` | (_available memory in the JVM i.e. Maximum heap size (`Xmx`) minus used memory_)
`sys.memory.free`      |(_amount of free memory in the JVM_)
`sys.memory.max`       |(_max amount of memory that the JVM will attempt to use_)
`sys.memory.total`     |(_total amount of memory in the JVM_)
`sys.memory.used`      |(_total minus free memory_)

__sys.memory__ supported input formats: `digits`, `digits[B|KB|MB|GB|TB|PB|EB|ZB|YB]` 
e.g `1000000`, `1_000_000`, `1GB`, `1gb`

### sys.time

name               | description
-------------------|------------
`sys.time`         | (_local date time_)
`sys.time.elapsed` |(_time elapsed since application start_)

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




 
