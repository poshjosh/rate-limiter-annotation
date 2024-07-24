package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.util.RateLimitProperties;

import java.lang.reflect.Method;

public interface RateProcessors {
    static RateProcessor<Class<?>> ofDefaults() {
        return ofClass();
    }

    static RateProcessor<RateLimitProperties> ofProperties() {
        return new PropertyRateProcessor();
    }

    static RateProcessor<Class<?>> ofClass() {
        return ofClass(RateProcessor.SourceFilter.ofRateLimited());
    }

    static RateProcessor<Method> ofMethod() {
        return ofMethod(RateProcessor.SourceFilter.ofRateLimited());
    }

    static RateProcessor<Class<?>> ofClass(RateProcessor.SourceFilter sourceTest) {
        return ofClass(sourceTest, AnnotationConverter.ofDefaults());
    }

    static RateProcessor<Method> ofMethod(RateProcessor.SourceFilter sourceTest) {
        return ofMethod(sourceTest, AnnotationConverter.ofDefaults());
    }

    static RateProcessor<Class<?>> ofClass(RateProcessor.SourceFilter sourceTest,
            AnnotationConverter annotationConverter) {
        return new ClassRateAnnotationProcessor(sourceTest, annotationConverter);
    }

    static RateProcessor<Method> ofMethod(RateProcessor.SourceFilter sourceTest,
            AnnotationConverter annotationConverter) {
        return new MethodRateAnnotationProcessor(sourceTest, annotationConverter);
    }
}
