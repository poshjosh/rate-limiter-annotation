# Developer Notes

### Releasing to maven cental

Release to maven central using the following command:

```shell
mvn clean deploy -Prelease -Dgpg.passphrase=[YOUR GPG PASSPHRASE]
```

After releasing browse to `https://s01.oss.sonatype.org/#stagingRepositories` and manually 
confirm the release. 

__Why do we currently use a tree structure for our rate limiters?__

Rate limiters could be created per method (for javaee within a DynamicFeature) or
per path pattern (for spring using `WebMvcConfigurer` e.g `registry.addInterceptor(new RequestInterceptor()) .addPathPatterns("/**")`)

However, we choose to use our own tree structure for rate limiters to improve flexibility.

We can thus:

- Add path pattern matchers to random points in our tree.

- Create rate limit groups that do not adhere to single-class-contains-one-or-more-methods paradigm.
  For example a rate limit group could span the methods of multiple classes.

__Why not simply add a `group` field to the `@Rate` annotation, instead of having a
separate `@RateGroup` annotation?__

A `@RateGroup` has an `operator` field. Adding this to the `@Rate` annotation means 
that 2 `@Rate` annotations on a single class could have different operators.


