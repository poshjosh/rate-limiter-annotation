# RateCondition Expression Language

A language for expressing the condition for rate limiting.

Examples:

```java
@Rate(5)
@RateCondition("sys.memory.available<1GB")
class Resource{ }
```

Support must be provided for the expression. Support is provided by default for the following:

sys.memory supported input formats: `digits`, `digits[B|KB|MB|GB|TB|PB|EB|ZB|YB]`

`sys.memory.available`

`sys.memory.free`

`sys.memory.max`

`sys.memory.total`

`sys.memory.used`

sys.time supported input formats: ISO-8601 Time formats:
`uuuu-MM-dd'T'HH:mm`
`uuuu-MM-dd'T'HH:mm:ss`
`uuuu-MM-dd'T'HH:mm:ss.SSS`
`uuuu-MM-dd'T'HH:mm:ss.SSSSSS`
`uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS`

`sys.time.elapsed`

`sys.time`

__Supported Operators__

`=`  EQUALS

`>`  GREATER

`>=` GREATER_OR_EQUALS

`<`  LESS

`<=` LESS_OR_EQUALS

`%`  LIKE

`^`  STARTS_WITH

`$`  ENDS_WITH

`!`  NOT (Negates other operators e.g `!=` or `!%`)




 
