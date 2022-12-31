# Developer Notes

__Why do we currently use a tree structure for our rate limiters?__

Rate limiters could be created per method (for javaee within a DynamicFeature) or
per path pattern (for spring using `WebMvcConfigurer` e.g `registry.addInterceptor(new RequestInterceptor()) .addPathPatterns("/**")`)

However, we choose to use our own tree structure for rate limiters to improve flexibility.

We can thus:

- Add path pattern matchers to random points in our tree.

- Create rate limit groups that do not adhere to single-class-contains-one-or-more-methods paradigm.
  For example a rate limit group could span the methods of multiple classes.

__Why not simply add a `group` field to the `@RateLimit` annotation, instead of having a
separate `@RateLimitGroup` annotation?__

A `@RateLimitGroup` has an `operator` field. Adding this to the `@RateLimit` annotation means 
that 2 `@RateLimit` annotations on a single class could have different operators.


