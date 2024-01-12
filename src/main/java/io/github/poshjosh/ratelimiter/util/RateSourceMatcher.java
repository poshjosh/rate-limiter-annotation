package io.github.poshjosh.ratelimiter.util;

import io.github.poshjosh.ratelimiter.annotation.ElementId;

import java.lang.reflect.Method;
import java.util.Objects;

final class RateSourceMatcher<INPUT> implements Matcher<INPUT> {
    private final String id;
    RateSourceMatcher(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public String match(INPUT input) {

        if (input == null) {
            return Matcher.NO_MATCH;
        }

        final String sval = input.toString();

        if (Objects.equals(id, sval)) {
            return sval;
        }

        final String classPart = ElementId.parseClassPart(sval).orElse(null);
        if (Objects.equals(id, classPart)) {
            return classPart;
        }

        if (input instanceof Class) {
            final String match = matchClass((Class)input);
            if (Matcher.isMatch(match)) {
                return match;
            }
        }

        if (input instanceof Method) {
            final Method method = (Method)input;
            String match = matchMethod(method);
            if (Matcher.isMatch(match)) {
                return match;
            }
            match = matchClass(method.getDeclaringClass());
            if (Matcher.isMatch(match)) {
                return match;
            }
        }

        return Matcher.NO_MATCH;
    }

    private String matchClass(Class<?> clazz) {
        final String candidate = ElementId.of(clazz);
        if (Objects.equals(id, candidate)) {
            return candidate;
        }
        return Matcher.NO_MATCH;
    }

    private String matchMethod(Method method) {
        final String candidate = ElementId.of(method);
        if (Objects.equals(id, candidate)) {
            return candidate;
        }
        return Matcher.NO_MATCH;
    }

    @Override
    public String toString() {
        return "RateSourceMatcher{" + "id='" + id + '\'' + '}';
    }
}
