package io.github.poshjosh.ratelimiter.matcher;

final class Checks {
    private Checks() { }
    static RuntimeException notSupported(Object complainer, Object unsupported) {
        return notSupported(complainer.getClass(), unsupported);
    }
    static RuntimeException notSupported(Class<?> complainer, Object unsupported) {
        return new UnsupportedOperationException(
                complainer.getSimpleName() + " does not support: " + unsupported);
    }
    static String requireContent(String s) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException("May not be null or empty: " + s);
        }
        return s;
    }
}
