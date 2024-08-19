package io.github.poshjosh.ratelimiter;

import io.github.poshjosh.ratelimiter.annotation.JavaRateSources;
import io.github.poshjosh.ratelimiter.annotation.RateId;
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

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterRegistryTest {

    private static final String ID = "test";

    @io.github.poshjosh.ratelimiter.annotations.Rate("1/s")
    @RateGroup
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @interface Limited { }

    @Limited
    static class ClassWithGroupLimits { }

    @io.github.poshjosh.ratelimiter.annotations.Rate("1/s")
    static class ClassWithLimits {}
    static class ClassWithNoLimits {}

    @ParameterizedTest
    @ValueSource(classes = { ClassWithLimits.class, ClassWithGroupLimits.class })
    void deregister_shouldDeregisterClassWithLimits(Class<?> clazz) {
        RateLimiterRegistry<?> registry = givenRegistry();
        assertTrue(registry.register(clazz).isRegistered(clazz));
        assertFalse(registry.deregister(RateId.of(clazz)).isRegistered(clazz));
    }

    @Test
    void deregister_shouldDeregisterIdWithLimits() {
        RateLimiterRegistry<?> registry = givenRegistry();
        final String id = "test-id";
        assertTrue(registry.register(Rates.of(id, Rate.ofSeconds(1))).isRegistered(id));
        assertFalse(registry.deregister(id).isRegistered(id));
    }

    @Test
    void register_shouldFailGivenAlreadyRegistered() {
        RateLimiterRegistry<?> registry = givenRegistry();
        final String id = "test-id";
        registry.register(Rates.of(id, Rate.ofSeconds(1)));
        assertThrows(UnsupportedOperationException.class,
                () -> registry.register(Rates.of(id, Rate.ofSeconds(1))));
    }
    
    @ParameterizedTest
    @ValueSource(classes = { ClassWithLimits.class, ClassWithGroupLimits.class })
    void register_shouldRegisterClassWithLimits(Class<?> clazz) {
        RateLimiterRegistry<?> registry = givenRegistry();
        assertTrue(registry.register(clazz).isRegistered(clazz));
    }

    @Test
    void register_shouldRegisterIdWithLimits() {
        final String id = "test-id";
        RateLimiterRegistry<?> registry = givenRegistry();
        assertTrue(registry.register(Rates.of(id, Rate.ofSeconds(1))).isRegistered(id));
    }

    @Test
    void register_shouldRegisterClassWithNoLimits() {
        Class<?> clazz = ClassWithNoLimits.class;
        RateLimiterRegistry<?> registry = givenRegistry();
        assertTrue(registry.register(clazz).isRegistered(clazz));
    }

    @Test
    void register_shouldRegisterIdWithNoLimits() {
        final String id = "test-id";
        RateLimiterRegistry<?> registry = givenRegistry();
        assertTrue(registry.register(Rates.ofId(id)).isRegistered(id));
    }

    @Test
    void register_shouldRegister_givenNoneRootParent() {
        final Rates parent = givenParentRates(1);
        register_shouldRegister_givenParent(parent);
    }

    @Test
    void register_shouldRegister_givenNoneRootRatelessParent() {
        final Rates parent = givenParentRates(-1);
        register_shouldRegister_givenParent(parent);
    }

    private Rates givenParentRates(int permits) {
        final String parentId = "test-parent-id";
        return permits < 1 ? Rates.ofId(parentId) : Rates.of(parentId, Rate.ofSeconds(permits));
    }

    private void register_shouldRegister_givenParent(Rates parent) {
        RateLimiterRegistry<?> registry = givenRegistry();
        final String parentId = parent.getId();
        assertTrue(registry.register(parent).isRegistered(parentId));
        System.out.println(registry);
        final String childId = "test-child-id";
        final Rates child = new Rates().parentId(parentId).id(childId).rates(Rate.ofSeconds(1));
        System.out.println(child);
        assertTrue(registry.register(child).isRegistered(childId));
        System.out.println(registry);
    }

    @ParameterizedTest
    @ValueSource(classes = { ClassWithLimits.class, ClassWithGroupLimits.class })
    void isRegistered_shouldReturnTrue_givenRegistryHasClassWithLimits(Class<?> clazz) {
        RateLimiterRegistry<?> registry = givenRegistryHavingClass(clazz);
        assertTrue(registry.isRegistered(clazz));
    }

    @Test
    void isRegistered_shouldReturnTrue_givenRegistryHasIdWithLimits() {
        final String id = "test-id";
        RateLimiterRegistry<?> registry = givenRegistryHavingRates(Rates.of(id, Rate.ofSeconds(1)));
        assertTrue(registry.isRegistered(id));
    }

    @Test
    void isRegistered_shouldReturnFalse_givenRegistryHasClassWithNoLimits() {
        Class<?> clazz = ClassWithNoLimits.class;
        RateLimiterRegistry<?> registry = givenRegistryHavingClass(clazz);
        assertFalse(registry.isRegistered(clazz));
    }

    // TODO - Fix this test
    @Test
    @Disabled
    void isRegistered_shouldReturnFalse_givenRegistryHasIdWithNoLimits() {
        final String id = "test-id";
        RateLimiterRegistry<?> registry = givenRegistryHavingRates(Rates.ofId(id));
        assertFalse(registry.isRegistered(id));
    }

    @ParameterizedTest
    @ValueSource(classes = { ClassWithLimits.class, ClassWithGroupLimits.class })
    void getRateLimiter_shouldReturnRateLimiter_whenRegistryHasClassWithLimits(Class<?> clazz) {
        RateLimiterRegistry<?> registry = givenRegistryHavingClass(clazz);
        assertTrue(registry.getRateLimiterOptional(JavaRateSources.of(clazz)).isPresent());
    }

    @Test
    void getRateLimiter_shouldReturnRateLimiter_whenRegistryHasIdWithLimits() {
        final String id = "test-id";
        RateLimiterRegistry<String> registry = givenRegistryHavingRates(Rates.of(id, Rate.ofSeconds(1)));
        assertTrue(registry.getRateLimiterOptional(id).isPresent());
    }

    @Test
    void getRateLimiter_shouldReturnEmpty_whenRegistryHasClassWithNoLimits() {
        Class<?> clazz = ClassWithNoLimits.class;
        RateLimiterRegistry<?> registry = givenRegistryHavingClass(clazz);
        assertFalse(registry.getRateLimiterOptional(JavaRateSources.of(clazz)).isPresent());
    }

    // TODO - Fix this test
    @Test
    @Disabled
    void getRateLimiter_shouldReturnEmpty_whenRegistryHasIdWithNoLimits() {
        final String id = "test-id";
        RateLimiterRegistry<String> registry = givenRegistryHavingRates(Rates.ofId(id));
        //System.out.println(registry.getRateLimiterOrUnlimited(id));
        assertFalse(registry.getRateLimiterOptional(id).isPresent());
    }

    @Test
    void isWithinLimit_andTryAcquire() {
        final Class<ClassWithLimits> clazz = ClassWithLimits.class;
        final String key = RateId.of(clazz);
        RateLimiterRegistry<String> registry = givenRegistryHavingClass(clazz);
        assertTrue(registry.isWithinLimit(key));
        assertTrue(registry.tryAcquire(key, 10));
        assertFalse(registry.isWithinLimit(key));
        assertFalse(registry.tryAcquire(key, 1));
        assertFalse(registry.isWithinLimit(key));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyPackagesSpecified() {
        assertNotNull(givenRegistryForPackage("dummy-package").requireRateLimiter(ID));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyClassesSpecified() {
        assertNotNull(givenRegistryHavingClass(ClassWithNoLimits.class).requireRateLimiter(ID));
    }

    @Test
    void shouldCreateRateLimiterWhenOnlyRatesSpecified() {
        RateLimiterContext<Object> context = RateLimiterContext.builder()
                .rates(Collections.singletonList(Rates.of(ID, Rate.ofSeconds(1))))
                .build();
        assertNotNull(RateLimiterRegistries.of(context).requireRateLimiter(ID));
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
        assertNotNull(RateLimiterRegistries.of(context).requireRateLimiter(ID));
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

    private RateLimiterRegistry givenRegistryHavingRates(Rates rates) {
        RateLimiterContext context = RateLimiterContext.builder()
                .rates(Collections.singletonList(rates)).build();
        //System.out.println(context);
        return RateLimiterRegistries.of(context);
    }
}