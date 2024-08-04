package io.github.poshjosh.ratelimiter.annotation;

import io.github.poshjosh.ratelimiter.annotations.Rate;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;

final class RateLimitedSourceTest implements RateProcessor.SourceFilter {

    RateLimitedSourceTest() { }

    @Override
    public boolean test(GenericDeclaration annotatedElement) {
        if (annotatedElement instanceof Class) {
            return isClassRateLimited((Class)annotatedElement);
        }
        return isRateLimited(annotatedElement);
    }

    protected boolean isClassRateLimited(Class<?> clazz) {
        do{
            if (isRateLimited(clazz)) {
                return true;
            }
            Method[] methods = clazz.getDeclaredMethods();
            for(Method method : methods) {
                if (isRateLimited(method)) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        }while(clazz != null && !clazz.equals(Object.class));
        return false;
    }

    protected boolean isRateLimited(GenericDeclaration source) {
        return hasRateAnnotations(source) || hasRateMetaAnnotation(source);
    }
    private boolean hasRateAnnotations(GenericDeclaration source) {
        return source.getAnnotationsByType(Rate.class).length > 0;
    }
    private boolean hasRateMetaAnnotation(GenericDeclaration source) {
        return Util.getMetaAnnotationTypeOrNull(source, Rate.class) != null;
    }
}
