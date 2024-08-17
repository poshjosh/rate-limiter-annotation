package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotation.exceptions.AnnotationProcessingException;
import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.annotations.RateGroup;
import io.github.poshjosh.ratelimiter.model.RateSource;
import io.github.poshjosh.ratelimiter.model.Rates;
import io.github.poshjosh.ratelimiter.model.Operator;
import io.github.poshjosh.ratelimiter.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class JavaRateSources {
    private JavaRateSources() { }

    public static RateSource of(GenericDeclaration source) {
        if (source instanceof Class) {
            return of((Class<?>)source);
        }
        if (source instanceof Method) {
            return of((Method)source);
        }
        throw new UnsupportedOperationException("Unsupported source type: " + source);
    }

    public static RateSource of(Class<?> clazz) {
        if (clazz.isAnnotation()) {
            return ofAnnotation(clazz);
        }
        return new ClassRateSource(clazz);
    }

    public static RateSource of(Method method) {
        return new MethodRateSource(method);
    }

    public static RateSource ofAnnotation(Class<?> source) {
        if (!source.isAnnotation()) {
            throw new IllegalArgumentException("Source must be an annotation type");
        }
        return new AnnotationRateSource(source);
    }

    private static final RateProcessor.SourceFilter isRateLimited =
            RateProcessor.SourceFilter.ofRateLimited();

    private static final class ClassRateSource extends AbstractRateSource {
        private ClassRateSource(Class<?> clazz) {
            super(clazz);
        }
        @Override public Optional<RateSource> getDeclarer() {
            return Optional.of(this);
        }
    }

    private static final class MethodRateSource extends AbstractRateSource {
        private final RateSource declarer;
        private MethodRateSource(Method method) {
            super(method);
            this.declarer = of(method.getDeclaringClass());
        }
        @Override public Optional<RateSource> getDeclarer() { return Optional.of(declarer); }
    }

    private static final class AnnotationRateSource extends AbstractRateSource {
        private AnnotationRateSource(Class<?> source) {
            super(source);
        }
        @Override public boolean isGroupType() { return true; }
    }

    protected abstract static class AbstractRateSource implements RateSource {
        private final String id;
        private final GenericDeclaration source;
        private final Rates rates;
        private final boolean rateLimited;
        protected AbstractRateSource(GenericDeclaration source) {
            this.id = RateId.of(source);
            this.source = Objects.requireNonNull(source);
            this.rateLimited = isRateLimited.test(source);
            this.rates = initRates();
        }
        @Override public String getId() {
            return id;
        }
        @Override public GenericDeclaration getSource() { return source; }
        @Override public Rates getRates() { return rates; }
        @Override public boolean isRateLimited() { return rateLimited; }
        @Override public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
            return Optional.ofNullable(source.getAnnotation(annotationClass));
        }

        private Rates initRates() {
            if (!isRateLimited()) {
                return Rates.none();
            }
            final RateGroup rateGroup = source.getAnnotation(RateGroup.class);
            final Rate[] rateAnnotations = source.getAnnotationsByType(Rate.class);
            final String conditionForAllRates = getCondition(source);

            final Operator operator = operator(rateGroup);
            validate(source, operator, rateAnnotations);
            if (rateAnnotations.length == 0) {
                // Operator is irrelevant for a single Rate
                return Rates.ofCondition(conditionForAllRates);
            }
            final io.github.poshjosh.ratelimiter.model.Rate[] rateData = new io.github.poshjosh.ratelimiter.model.Rate[rateAnnotations.length];
            for (int i = 0; i < rateAnnotations.length; i++) {
                rateData[i] = convert(rateAnnotations[i]);
            }
            if (rateData.length == 1) {
                final io.github.poshjosh.ratelimiter.model.Rate only = rateData[0];
                if (!StringUtils.hasText(conditionForAllRates)) {
                    return Rates.of(only);
                }
                if (!StringUtils.hasText(only.getCondition())) {
                    only.setCondition(conditionForAllRates);
                    return Rates.of(only);
                }
            }

            return Rates.of(id, operator, conditionForAllRates, rateData);
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

        private String getCondition(GenericDeclaration source) {
            final RateCondition condition = source.getAnnotation(RateCondition.class);
            return getExpression(source, condition);
        }

        private String getExpression(GenericDeclaration source, RateCondition condition) {
            return condition == null ? "" :
                    Checks.requireOneContent(source, "RateCondition expression",
                            condition.expression(), condition.value());
        }

        private Operator operator(RateGroup rateGroup) {
            return rateGroup == null ? Operator.NONE : rateGroup.operator();
        }

        private io.github.poshjosh.ratelimiter.model.Rate convert(Rate rate) {
            final long permits = rate.permits();
            final String rateText = rate.value().isEmpty() ? rate.rate() : rate.value();
            if (permits == -1 && (rateText == null || rateText.isEmpty())) {
                throw new AnnotationProcessingException(
                        "Either `permits` or `rate` must be specified in: " + rate);
            }
            Duration duration = rate.duration() == 0 ?
                    Duration.ZERO : Duration.of(rate.duration(), toChronoUnit(rate.timeUnit()));
            String when = StringUtils.hasText(rate.condition()) ? rate.condition() : rate.when();
            String factoryClass = rate.factoryClass().getName();
            if (StringUtils.hasText(rateText)) {
                return io.github.poshjosh.ratelimiter.model.Rate.of(rateText)
                        .condition(when).factoryClass(factoryClass);
            }
            return io.github.poshjosh.ratelimiter.model.Rate
                    .of(permits, duration, when, factoryClass);
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
        @Override public int hashCode() { return Objects.hashCode(getId()); }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RateSource)) {
                return false;
            }
            return getId().equals(((RateSource)o).getId());
        }
        @Override public String toString() {
            return this.getClass().getSimpleName() + '{' + getId() + '}';
        }
    }
}
