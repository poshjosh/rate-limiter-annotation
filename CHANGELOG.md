# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [ Unreleased ]

-

## [ [0.5.0](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.5.0) ] - 2023-05-01

### Added

- Allow usage of 3rd party rate limiters

## [ [0.4.2](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.4.2) ] - 2023-02-24

### Added

- Use system epoch millis as Ticker time

## [ [0.4.1](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.4.1) ] - 2023-02-19

### Added

- Added expression handling for `sys.property` and `sys.environment`
- Added `UnmodifiableRegistries`
- Added methods `ResourceLimiterRegistry#register` taking either `Class` or `Method` argument. This enables the manual registration of resources for rate limting.

### Changed

- Modified `ResourceLimiterRegisry#getRateConfig` to `getLimiterConfig`
- Updated README. Note that resource packages/classes must be specified for automatic rate limiting to work.

### Removed

- Removed dependency on `javax.cache.Cache`

## [ [0.4.0](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.4.0) ] - 2023-02-11

### Changed

- Fixed handling of multiple (non-composed) limits on a single node.
- Fixed handling of multiple methods with same path but different http method.
- Rewrote BandwidthStore to accept Bandwidth, rather than Bandwidth[].

## [ [0.3.4](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.3.4) ] - 2023-02-05

### Changed

- Permit expressions (for rate conditions) having `null` right-hand side
- Improved expression splitting
- Renamed `web.session.cookie` to `web.request.cookie`
- Renamed `web.session.user.role` to `web.request.user.role`
- Renamed `web.session.user.principal` to `web.request.user.principal`
- Renamed `Element` to `RateSource` and add `PropertyRateSource`
- Renamed `Matcher#matchOrNull` to `Matcher#match`
- Use String return type for `Matcher#match`

## [ [0.3.3](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.3.3) ] - 2023-02-04

### Added

- Added field `id` to `Bandwidths`
- Added support for multiple (non-composed) bandwidths

### Changed

- Renamed `Operator#DEFAULT` to `Operator#NONE`

## [ [0.3.2](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.3.2) ] - 2023-01-29

### Added

- Added `when` field to `@Rate` annotation. It is an alias for `@RateCondition`

### Changed

- Moved `ResourceLimiter` from [rate-limiter](https://github.com/poshjosh/rate-limiter) to [rate-limiter-annotation](https://github.com/poshjosh/rate-limiter-annotation).

### Removed

- Removed `ResourceLimiters.of`. Rather use: `ResourceLimiter.of`

## [ [0.3.1](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.3.1) ] - 2023-01-24

### Added

- Implement JVM thread rate condition expression, setup code coverage

## [ [0.3.0](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.3.0) ] - 2023-01-22

### Added

- Implement `@RateCondition` using expressions
- Add method `getResourceClasses` to `RateLimitProperties`
- Add a default operator i.e `Operator.DEFAULT`
- Add more tests
- Improve performance

### Changed

- Restrict `@RateGroup` annotation to `ElementType.ANNOTATION_TYPE`
- Rename `MatchedResourceLimiter` to `ResourceLimiters`
- Make `@RateGroup#name` or `@RateGroup#value` mandatory.
- Rename `Bandwidths.getMembers` to `Bandwidths.getBandwidths`

### Removed

- Remove annotation `@RateReqestIf`

## [ [0.2.0](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.2.0) ] - 2023-01-08

### Added

- Implement matching of requests via annotation `@RateRequestIf`
- Add alias `value` as alias for `permits` property in `@Rate`

### Changed

- Rename package `com.looseboxes` to `io.github.poshjosh`
- Move `@Rate` and `@RateGroup` from package `ratelimiter.annotations` to `ratelimiter.annotation`

### Removed

- Remove methods `RateCache.cache` and `RateCache.listener`

## [ [0.1.0](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.1.0) ] - 2023-01-07

### Added

- Add `name` field to `@RateLimit` annotation
- Add builder like methods to `ResourceLimiter` for setting `RateCache` and `UsageListener`
- Support single `Rate` per config in properties. Was previously a list of `Rate`s

### Changed

- Update README.md
- Rename all `of()` to `ofDefaults()`
- Change `@RateLimit.limit` to `@RateLimit.permits`
- Change default `timeUnit` in `@RateLimit` to `TimeUnit.SECONDS`
- Rename method `AnnotationProcessor.ofRates` to `AnnotationProcessor.ofDefaults`
- Rename `PatternMatchingResourceLimiter` to `MatchedResourceLimiter`
- Rename `RateLimit` annotation to `Rate`
- Rename `RateLimitGroup` to `RateGroup`
- Change default `@Rate.duration` to 1

### Removed

- Remove annotation `@Nullable`
- Remove the value-related generic from `RateCache`. The value now has a fixed type of `Bandwidths`

## [ [0.0.9](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.0.9) ] - 2022-12-30

### Added

- Add more Bandwidth tests

## [ [0.0.8](https://github.com/poshjosh/rate-limiter-annotation/tree/v0.0.8) ] - 2022-12-29

### Added

- Separate repository for annotations: [rate-limiter-annotation](https://github.com/poshjosh/rate-limiter-annotation)
- Change log
