package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotation.exceptions.AnnotationProcessingException;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.util.StringUtils;

import java.lang.reflect.GenericDeclaration;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

final class RateAnnotationConverter implements AnnotationConverter {

    RateAnnotationConverter() { }

    @Override
    public Class<Rate> getAnnotationType() {
        return Rate.class;
    }

    @Override
    public Rates convert(GenericDeclaration source) {
        final RateGroup rateGroup = source.getAnnotation(RateGroup.class);
        final Rate[] rates = source.getAnnotationsByType(getAnnotationType());
        final String rateConditionForAllRates = getRateCondition(source);

        final Operator operator = operator(rateGroup);
        validate(source, operator, rates);
        if (rates.length == 0) {
            // Operator is irrelevant for a single Rate
            return Rates.of(rateConditionForAllRates);
        }
        final io.github.poshjosh.ratelimiter.model.Rate[] rateData = new io.github.poshjosh.ratelimiter.model.Rate[rates.length];
        for (int i = 0; i < rates.length; i++) {
            rateData[i] = convert(rates[i]);
        }
        if (rateData.length == 1) {
            final io.github.poshjosh.ratelimiter.model.Rate only = rateData[0];
            if (!StringUtils.hasText(rateConditionForAllRates)) {
                return Rates.of(only);
            }
            if (!StringUtils.hasText(only.getRateCondition())) {
                only.setRateCondition(rateConditionForAllRates);
                return Rates.of(only);
            }
        }

        return Rates.of(operator, rateConditionForAllRates, rateData);
    }

    private void validate(GenericDeclaration source, Operator operator, Rate[] rates) {
        if (Operator.NONE.equals(operator)) {
            return;
        }
        // Tag:Rule:Operator-may-not-be-specified-when-multiple-rate-conditions-are-specified
        for (Rate rate : rates) {
            if (StringUtils.hasText(rate.condition()) || StringUtils.hasText(rate.when())) {
                throw new AnnotationProcessingException(
                        "Operator may not be specified, when multiple rate conditions are specified; at: " + source);
            }
        }
    }

    private String getRateCondition(GenericDeclaration source) {
        final RateCondition rateCondition = source.getAnnotation(RateCondition.class);
        return getExpression(source, rateCondition);
    }

    private String getExpression(GenericDeclaration source, RateCondition rateCondition) {
        return rateCondition == null ? "" :
                Checks.requireOneContent(source, "RateCondition expression",
                rateCondition.expression(), rateCondition.value());
    }

    private Operator operator(RateGroup rateGroup) {
        return rateGroup == null ? Operator.NONE : rateGroup.operator();
    }

    private io.github.poshjosh.ratelimiter.model.Rate convert(Rate rate) {
        long value = rate.value() == Long.MAX_VALUE ? rate.permits() : rate.value();
        Duration duration = Duration.of(rate.duration(), toChronoUnit(rate.timeUnit()));
        String condition = StringUtils.hasText(rate.condition()) ? rate.condition() : rate.when();
        return io.github.poshjosh.ratelimiter.model.Rate
                .of(value, duration, condition, rate.factoryClass());
    }

    private ChronoUnit toChronoUnit(TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit);
        if (TimeUnit.NANOSECONDS.equals(timeUnit)) {
            return ChronoUnit.NANOS;
        }
        if (TimeUnit.MICROSECONDS.equals(timeUnit)) {
            return ChronoUnit.MICROS;
        }
        if (TimeUnit.MILLISECONDS.equals(timeUnit)) {
            return ChronoUnit.MILLIS;
        }
        if (TimeUnit.SECONDS.equals(timeUnit)) {
            return ChronoUnit.SECONDS;
        }
        if (TimeUnit.MINUTES.equals(timeUnit)) {
            return ChronoUnit.MINUTES;
        }
        if (TimeUnit.HOURS.equals(timeUnit)) {
            return ChronoUnit.HOURS;
        }
        if (TimeUnit.DAYS.equals(timeUnit)) {
            return ChronoUnit.DAYS;
        }
        throw Checks.illegal(timeUnit);
    }
}
