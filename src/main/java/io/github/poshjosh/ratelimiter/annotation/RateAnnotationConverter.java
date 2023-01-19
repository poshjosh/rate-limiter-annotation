package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.util.Rates;

import java.lang.reflect.GenericDeclaration;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class RateAnnotationConverter implements AnnotationConverter<Rate, Rates> {

    RateAnnotationConverter() { }

    @Override
    public Class<Rate> getAnnotationType() {
        return Rate.class;
    }

    @Override
    public Rates convert(GenericDeclaration source) {
        final RateGroup rateGroup = source.getAnnotation(RateGroup.class);
        final Rate[] rates = source.getAnnotationsByType(getAnnotationType());
        final RateCondition rateCondition = source.getAnnotation(RateCondition.class);
        final String expression = rateCondition == null ? "" : getExpression(rateCondition, source);

        final Operator operator = operator(rateGroup);
        if (rates.length == 0) {
            return Rates.of(operator).rateCondition(expression);
        }
        final io.github.poshjosh.ratelimiter.util.Rate[] configs = new io.github.poshjosh.ratelimiter.util.Rate[rates.length];
        for (int i = 0; i < rates.length; i++) {
            configs[i] = convert(rates[i]);
        }
        return Rates.of(operator, configs).rateCondition(expression);
    }

    private String getExpression(RateCondition rateGroup, GenericDeclaration source) {
        return Checks.requireOneContent(source, "RateCondition expression",
                rateGroup.expression(), rateGroup.value());
    }

    private Operator operator(RateGroup rateGroup) {
        return rateGroup == null ? Operator.DEFAULT : rateGroup.operator();
    }

    protected io.github.poshjosh.ratelimiter.util.Rate convert(Rate rate) {
        final long value = rate.value() == Long.MAX_VALUE ? rate.permits() : rate.value();
        Duration duration = Duration.of(rate.duration(), toChronoUnit(rate.timeUnit()));
        return io.github.poshjosh.ratelimiter.util.Rate
                .of(value, duration, rate.factoryClass());
    }

    protected ChronoUnit toChronoUnit(TimeUnit timeUnit) {
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
