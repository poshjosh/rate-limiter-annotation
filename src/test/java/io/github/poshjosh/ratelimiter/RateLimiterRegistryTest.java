package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.model.Rate;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.RateLimitProperties;
import org.junit.jupiter.api.Disabled;
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
import java.util.Map;

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
    void register_shouldRegisterIdWithLimits() {
        final String id = "test-id";
        RateLimiterRegistry registry = givenRegistry();
        assertTrue(registry.register(id, Rate.ofSeconds(1)).isRegistered(id));
    }

    @Test
    void register_shouldNotRegisterClassWithNoLimits() {
        Class<?> clazz = ClassWithNoLimits.class;
        RateLimiterRegistry registry = givenRegistry();
        assertFalse(registry.register(clazz).isRegistered(clazz));
    }

    @Test
    void register_shouldNotRegisterIdWithNoLimits() {
        final String id = "test-id";
        RateLimiterRegistry registry = givenRegistry();
        assertFalse(registry.register(id, Rates.none()).isRegistered(id));
    }

    @ParameterizedTest
    @ValueSource(classes = { ClassWithLimits.class, ClassWithGroupLimits.class })
    void isRegistered_shouldReturnTrue_givenRegistryHasClassWithLimits(Class<?> clazz) {
        RateLimiterRegistry registry = givenRegistryHavingClass(clazz);
        assertTrue(registry.isRegistered(clazz));
    }

    @Test
    void isRegistered_shouldReturnTrue_givenRegistryHasIdWithLimits() {
        final String id = "test-id";
        RateLimiterRegistry registry = givenRegistryHavingRates(id, Rates.of(Rate.ofSeconds(1)));
        assertTrue(registry.isRegistered(id));
    }


    @Test
    void isRegistered_shouldReturnFalse_givenRegistryHasClassWithNoLimits() {
        Class<?> clazz = ClassWithNoLimits.class;
        RateLimiterRegistry registry = givenRegistryHavingClass(clazz);
        assertFalse(registry.isRegistered(clazz));
    }

    // TODO - Fix this test
    @Test
    @Disabled
    void isRegistered_shouldReturnFalse_givenRegistryHasIdWithNoLimits() {
        final String id = "test-id";
        RateLimiterRegistry registry = givenRegistryHavingRates(id, Rates.none());
        assertFalse(registry.isRegistered(id));
    }

    @ParameterizedTest
    @ValueSource(classes = { ClassWithLimits.class, ClassWithGroupLimits.class })
    void getRateLimiter_shouldReturnRateLimiter_whenRegistryHasClassWithLimits(Class<?> clazz) {
        RateLimiterRegistry registry = givenRegistryHavingClass(clazz);
        assertTrue(registry.getClassRateLimiterOptional(clazz).isPresent());
    }

    @Test
    void getRateLimiter_shouldReturnRateLimiter_whenRegistryHasIdWithLimits() {
        final String id = "test-id";
        RateLimiterRegistry registry = givenRegistryHavingRates(id, Rates.of(Rate.ofSeconds(1)));
        assertTrue(registry.getRateLimiterOptional(id).isPresent());
    }

    @Test
    void getRateLimiter_shouldReturnEmpty_whenRegistryHasClassWithNoLimits() {
        Class<?> clazz = ClassWithNoLimits.class;
        RateLimiterRegistry registry = givenRegistryHavingClass(clazz);
        assertFalse(registry.getClassRateLimiterOptional(clazz).isPresent());
    }

    // TODO - Fix this test
    @Test
    @Disabled
    void getRateLimiter_shouldReturnEmpty_whenRegistryHasIdWithNoLimits() {
        final String id = "test-id";
        RateLimiterRegistry registry = givenRegistryHavingRates(id, Rates.none());
        //System.out.println(registry.getRateLimiterOrUnlimited(id));
        assertFalse(registry.getRateLimiterOptional(id).isPresent());
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyPackagesSpecified() {
        assertNotNull(givenRegistryForPackage("dummy-package").getRateLimiter(ID));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyClassesSpecified() {
        assertNotNull(givenRegistryHavingClass(ClassWithNoLimits.class).getRateLimiter(ID));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyRatesSpecified() {
        RateLimiterContext<Object> context = RateLimiterContext.builder()
                .rates(Collections.singletonMap(ID, Rates.of(Rate.ofSeconds(1))))
                .build();
        assertNotNull(RateLimiterRegistries.of(context).getRateLimiter(ID));
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
        assertNotNull(RateLimiterRegistries.of(context).getRateLimiter(ID));
    }

    private RateLimiterRegistry givenRegistry() {
        return givenRegistryForPackage("dummy-package");
    }

    private RateLimiterRegistry givenRegistryForPackage(String packageName) {
        RateLimiterContext context = RateLimiterContext.builder()
                .packages(packageName).build();
        return RateLimiterRegistries.of(context);
    }

    private RateLimiterRegistry givenRegistryHavingClass(Class<?> clazz) {
        RateLimiterContext context = RateLimiterContext.builder().classes(clazz).build();
        return RateLimiterRegistries.of(context);
    }

    private RateLimiterRegistry givenRegistryHavingRates(String id, Rates rates) {
        RateLimiterContext context = RateLimiterContext.builder()
                .rates(Collections.singletonMap(id, rates)).build();
        //System.out.println(context);
        return RateLimiterRegistries.of(context);
    }
}