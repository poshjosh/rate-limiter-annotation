package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterRegistryTest {

    private static final String ID = "test";

    @io.github.poshjosh.ratelimiter.annotations.Rate(1)
    @RateGroup
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @interface Limited { }

    @Limited
    static class ClassWithGroupLimits { }

    @io.github.poshjosh.ratelimiter.annotations.Rate(1)
    static class ClassWithLimits {}
    static class ClassWithNoLimits {}

    @ParameterizedTest
    @ValueSource(classes = { ClassWithLimits.class, ClassWithGroupLimits.class })
    void register_shouldRegisterClassWithLimits(Class<?> clazz) {
        RateLimiterRegistry registry = givenRegistry();
        assertTrue(registry.register(clazz).isRegistered(clazz));
    }

    @Test
    void register_shouldNotRegisterClassWithNoLimits() {
        Class<?> clazz = ClassWithNoLimits.class;
        RateLimiterRegistry registry = givenRegistry();
        assertFalse(registry.register(clazz).isRegistered(clazz));
    }

    @ParameterizedTest
    @ValueSource(classes = { ClassWithLimits.class, ClassWithGroupLimits.class })
    void isRegistered_shouldReturnTrue_whenRegistryHasClassWithLimits(Class<?> clazz) {
        RateLimiterRegistry registry = givenRegistryHavingClass(clazz);
        assertTrue(registry.isRegistered(clazz));
    }

    @Test
    void isRegistered_shouldReturnFalse_whenRegistryHasClassWithNoLimits() {
        Class<?> clazz = ClassWithNoLimits.class;
        RateLimiterRegistry registry = givenRegistryHavingClass(clazz);
        assertFalse(registry.isRegistered(clazz));
    }

    @ParameterizedTest
    @ValueSource(classes = { ClassWithLimits.class, ClassWithGroupLimits.class })
    void getRateLimiter_shouldReturnRateLimiter_whenRegistryHasClassWithLimits(Class<?> clazz) {
        RateLimiterRegistry registry = givenRegistryHavingClass(clazz);
        assertTrue(registry.getRateLimiter(clazz).isPresent());
    }

    @Test
    void getRateLimiter_shouldReturnEmpty_whenRegistryHasClassWithNoLimits() {
        Class<?> clazz = ClassWithNoLimits.class;
        RateLimiterRegistry registry = givenRegistryHavingClass(clazz);
        assertFalse(registry.getRateLimiter(clazz).isPresent());
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyPackagesSpecified() {
        RateLimiterFactory<Object> factory = givenRegistryForPackage("dummy-package")
                .createRateLimiterFactory();
        assertNotNull(factory);
        assertNotNull(factory.getRateLimiter(ID));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyClassesSpecified() {
        RateLimiterFactory<Object> factory = givenRegistryHavingClass(ClassWithNoLimits.class)
                .createRateLimiterFactory();
        assertNotNull(factory);
        assertNotNull(factory.getRateLimiter(ID));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyRatesSpecified() {
        RateLimiterContext<Object> context = RateLimiterContext.builder()
                .rates(Collections.singletonMap(ID, Rates.of(Rate.ofSeconds(1))))
                .build();
        RateLimiterFactory<Object> factory = RateLimiterRegistry.of(context).createRateLimiterFactory();
        assertNotNull(factory);
        assertNotNull(factory.getRateLimiter(ID));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyPropertiesSpecified() {
        RateLimitProperties properties = new RateLimitProperties() {
            @Override public List<Class<?>> getResourceClasses() {
                return Collections.emptyList();
            }
            @Override public List<String> getResourcePackages() { return Arrays.asList("package"); }
        };
        RateLimiterContext<Object> context = RateLimiterContext.builder()
                .properties(properties)
                .build();
        RateLimiterFactory<Object> factory = RateLimiterRegistry.of(context).createRateLimiterFactory();
        assertNotNull(factory);
        assertNotNull(factory.getRateLimiter(ID));
    }

    private RateLimiterRegistry givenRegistry() {
        return givenRegistryForPackage("dummy-package");
    }

    private RateLimiterRegistry givenRegistryForPackage(String packageName) {
        RateLimiterContext context = RateLimiterContext.builder()
                .packages(packageName).build();
        return RateLimiterRegistry.of(context);
    }

    private RateLimiterRegistry givenRegistryHavingClass(Class<?> clazz) {
        RateLimiterContext context = RateLimiterContext.builder().classes(clazz).build();
        return RateLimiterRegistry.of(context);
    }
}