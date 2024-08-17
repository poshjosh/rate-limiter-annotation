package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.model.Operator;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.model.Rates;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class JavaRateSourcesTest {

    static class ClassWithZeroRates {}

    @Test
    void getRates_givenClassWithZeroRates_shouldReturnEmptyRates() {
        Rates rates = JavaRateSources.of(ClassWithZeroRates.class).getRates();
        assertFalse(rates.isSet());
        assertEquals(Operator.NONE, rates.getOperator());
        assertTrue(rates.getRateCondition() == null || rates.getRateCondition().isEmpty());
    }

    @Rate(permits=7, duration=2, timeUnit=TimeUnit.MINUTES, condition="jvm.memory.free > 0")
    static class ClassWithSingleRate {}

    @Test
    void getRates_givenClassWithSingleRate_shouldReturnMatchingRate() {
        Rates rates = JavaRateSources.of(ClassWithSingleRate.class).getRates();
        assertEquals(1, rates.totalSize());
        io.github.poshjosh.ratelimiter.model.Rate rate = rates.getLimit();
        assertEquals(7, rate.getPermits());
        assertEquals(Duration.ofMinutes(2), rate.getDuration());
        assertEquals("jvm.memory.free > 0", rate.getCondition());
    }

    @Rate("2/s")
    @Rate("10/s")
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @RateGroup(id ="resource-group", operator = Operator.AND)
    public @interface CustomRateGroup { }

    @Rate("1/s")
    @Rate(permits=5, condition="sys.time.elapsed > PT0S")
    @RateCondition("jvm.memory.free < 0")
    @CustomRateGroup
    static class ClassWith2Rates {}

    @Test
    void getRates_givenClassWithSomeRates_shouldReturnSameNumberOfRates() {
        // 1 x RateCondition (main)
        // 2 x Rate (sub)
        Rates rates = JavaRateSources.of(ClassWith2Rates.class).getRates();
        assertEquals(3, rates.totalSize());
        assertEquals("jvm.memory.free < 0", rates.getRateCondition());
        assertTrue(rates.getSubLimits().stream()
                .map(io.github.poshjosh.ratelimiter.model.Rate::getCondition)
                .anyMatch("sys.time.elapsed > PT0S"::equals));
    }

    @Test
    void convert_givenGroupAnnotationWithSomeRates_shouldReturnSameNumberOfRates() {
      Rates rates = JavaRateSources.of(CustomRateGroup.class).getRates();
      assertEquals(2, rates.totalSize());
      assertEquals(Operator.AND, rates.getOperator());
    }
}