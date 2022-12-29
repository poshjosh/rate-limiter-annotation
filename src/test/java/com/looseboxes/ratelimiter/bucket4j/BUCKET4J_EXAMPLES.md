# Bucket4j Examples

### To create a RateLimiter based on bucket4j-jcache.

pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <dependencies>
        <dependency>
            <groupId>com.github.vladimir-bukhtoyarov</groupId>
            <artifactId>bucket4j-core</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>
        <dependency>
            <groupId>com.github.vladimir-bukhtoyarov</groupId>
            <artifactId>bucket4j-jcache</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>
    </dependencies>
</project>
```

Code

```java


public class Bucket4jJCacheRateLimiterProvider<K extends Serializable> {

    private static class JCacheProxyManagerProvider implements ProxyManagerProvider {

        @Override
        public <K extends Serializable> ProxyManager<K> getProxyManager(RateCache<K, ?> rateCache) {
            // It is possible to get a javax.cache.Cache via RateCache#unwrap(Class),
            // only if the RateCache is implemented over an javax.cache.Cache,
            // e.g using JavaRateCache
            return Bucket4j.extension(JCache.class).proxyManagerForCache(rateCache.unwrap(Cache.class));
        }
    }

    public RateLimiter<K> newInstance(Cache<K, GridBucketState> cache, Rate... rates) {
        ProxyManager<K> proxyManager = Bucket4j.extension(JCache.class).proxyManagerForCache(cache);
        return new Bucket4jRateLimiter<>(proxyManager, Rates.of(rates));
    }

    public RateLimiter<K> newInstanceFromAnnotatedClasses(Cache<K, GridBucketState> cache, Class<?>... classes) {
        return RateLimiterFromAnnotationFactory.<K, GridBucketState>of()
                .resourceLimiterFactory(new Bucket4jRateLimiterFactory<>(new ProxyManagerProviderImpl()))
                .resourceLimiterConfig(RateLimiterConfig.<K, GridBucketState>builder().rateCache(RateCache.of(cache)).build())
                .create(classes);
    }
}
```

### To create a RateLimiter based on bucket4j-hazelcast.

pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <dependencies>
        <dependency>
            <groupId>com.github.vladimir-bukhtoyarov</groupId>
            <artifactId>bucket4j-core</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>
        <dependency>
            <groupId>com.hazelcast</groupId>
            <artifactId>hazelcast</artifactId>
            <version>4.0.2</version>
        </dependency>
        <dependency>
            <groupId>com.github.vladimir-bukhtoyarov</groupId>
            <artifactId>bucket4j-hazelcast</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>
    </dependencies>
</project>
```

Code

```java
public class Bucket4jHazelcastRateLimiterProvider<K extends Serializable>{

    private static class ProxyManagerProviderImpl implements ProxyManagerProvider{
        @Override
        public <K extends Serializable> ProxyManager<K> getProxyManager(RateCache<K, ?> rateCache) {
            // It is possible to get a com.hazelcast.map.IMap via RateCache#unwrap(Class),
            // only if the RateCache is implemented over an com.hazelcast.map.IMap,
            // e.g using MapRateCache
            return Bucket4j.extension(Hazelcast.class).proxyManagerForMap(rateCache.unwrap(IMap.class));
        }
    }

    public RateLimiter<K> newInstance(IMap<K, GridBucketState> cache, Rate... rates) {
        ProxyManager<K> proxyManager = Bucket4j.extension(Hazelcast.class).proxyManagerForMap(cache);
        return new Bucket4jRateLimiter<>(proxyManager, Rates.of(rates));
    }

    public RateLimiter<K> newInstanceFromAnnotatedClasses(IMap<K, GridBucketState> cache, Class<?>... classes) {
        return RateLimiterFromAnnotationFactory.<K, GridBucketState>of()
                .resourceLimiterFactory(new Bucket4jRateLimiterFactory<>(new ProxyManagerProviderImpl()))
                .resourceLimiterConfig(RateLimiterConfig.<K, GridBucketState>builder().rateCache(RateCache.of(cache)).build())
                .create(classes);
    }
}
```

### To create a RateLimiter based on bucket4j-ignite.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <dependencies>
        <dependency>
            <groupId>com.github.vladimir-bukhtoyarov</groupId>
            <artifactId>bucket4j-core</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-core</artifactId>
            <version>2.11.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-indexing</artifactId>
            <version>2.11.1</version>
        </dependency>
        <dependency>
            <groupId>com.github.vladimir-bukhtoyarov</groupId>
            <artifactId>bucket4j-ignite</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>
    </dependencies>
</project>
```

Code

```java
public class Bucket4jIgniteRateLimiterProvider<K extends Serializable>{

    private static class ProxyManagerProviderImpl implements ProxyManagerProvider{
        @Override
        public <K extends Serializable> ProxyManager<K> getProxyManager(RateCache<K, ?> rateCache) {
            // It is possible to get a org.apache.ignite.IgniteCache via RateCache#unwrap(Class),
            // only if the RateCache is implemented over an org.apache.ignite.IgniteCache,
            // e.g using JavaRateCache
            return Bucket4j.extension(Ignite.class).proxyManagerForCache(rateCache.unwrap(IgniteCache.class));
        }
    }

    public RateLimiter<K> newInstance(IgniteCache<K, GridBucketState> cache, Rate... rates) {
        ProxyManager<K> proxyManager = Bucket4j.extension(Ignite.class).proxyManagerForCache(cache);
        return new Bucket4jRateLimiter<>(proxyManager, Rates.of(rates));
    }

    public RateLimiter<K> newInstanceFromAnnotatedClasses(IgniteCache<K, GridBucketState> cache, Class<?>... classes) {
        return RateLimiterFromAnnotationFactory.<K, GridBucketState>of()
                .resourceLimiterFactory(new Bucket4jRateLimiterFactory<>(new ProxyManagerProviderImpl()))
                .resourceLimiterConfig(RateLimiterConfig.<K, GridBucketState>builder().rateCache(RateCache.of(cache)).build())
                .create(classes);
    }
}
```

### To create a RateLimiter based on bucket4j-infinispan.

TODO

### To create a RateLimiter based on bucket4j-coherence.

TODO