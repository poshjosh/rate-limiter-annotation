package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.util.Rates;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationConverterTest {

    AnnotationConverter<Rate, Rates> annotationConverter = AnnotationConverter.ofRate();

    @Test
    void getAnnotationType() {
        assertEquals(Rate.class, annotationConverter.getAnnotationType());
    }

    static class ClassWithZeroRates {}

    @Test
    void convert_givenClassWithZeroRates_shouldReturnEmptyRates() {
        Rates rates = annotationConverter.convert(ClassWithZeroRates.class);
        assertFalse(rates.hasLimits());
        assertEquals(Operator.NONE, rates.getOperator());
        assertEquals("", rates.getRateCondition());
    }

    @Rate(permits=7, duration=2, timeUnit=TimeUnit.MINUTES, condition="jvm.memory.free>0")
    static class ClassWithSingleRate {}

    @Test
    void convert_givenClassWithSingleRate_shouldReturnMatchingRate() {
        Rates rates = annotationConverter.convert(ClassWithSingleRate.class);
        assertEquals(1, rates.size());
        io.github.poshjosh.ratelimiter.util.Rate rate = rates.getLimits().get(0);
        assertEquals(7, rate.getPermits());
        assertEquals(Duration.ofMinutes(2), rate.getDuration());
        assertEquals("jvm.memory.free>0", rate.getRateCondition());
    }

    @Rate(2)
    @Rate(10)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @RateGroup(name="resource-group", operator = Operator.AND)
    public @interface CustomRateGroup { }

    @Rate(1)
    @Rate(permits=5, condition="sys.time.elapsed>PT0S")
    @RateCondition("jvm.memory.free<0")
    @CustomRateGroup
    static class ClassWith2Rates {}

    @Test
    void convert_givenClassWithSomeRates_shouldReturnSameNumberOfRates() {
        Rates rates = annotationConverter.convert(ClassWith2Rates.class);
        assertEquals(2, rates.size());
        assertEquals("jvm.memory.free<0", rates.getRateCondition());
        assertTrue(rates.getLimits().stream()
                .map(rate -> rate.getRateCondition())
                .anyMatch("sys.time.elapsed>PT0S"::equals));
    }

    @Test
    void convert_givenGroupAnnotationWithSomeRates_shouldReturnSameNumberOfRates() {
      Rates rates = annotationConverter.convert(CustomRateGroup.class);
      assertEquals(2, rates.size());
      assertEquals(Operator.AND, rates.getOperator());
    }
}