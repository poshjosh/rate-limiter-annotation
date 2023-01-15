package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.util.Operator;
import io.github.poshjosh.ratelimiter.util.Rates;

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
    public Rates convert(RateGroup rateGroup, Element element, Rate[] rates) {
        final Operator operator = operator(rateGroup);
        if (rates.length == 0) {
            return Rates.of(operator);
        }
        final io.github.poshjosh.ratelimiter.util.Rate[] configs = new io.github.poshjosh.ratelimiter.util.Rate[rates.length];
        for (int i = 0; i < rates.length; i++) {
            configs[i] = convert(rates[i]);
        }
        return Rates.of(operator, configs);
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
        throw new IllegalArgumentException("Unexpected TimeUnit: " + timeUnit);
    }
}
