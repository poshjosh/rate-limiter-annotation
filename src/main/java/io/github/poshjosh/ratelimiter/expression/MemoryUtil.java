package io.github.poshjosh.ratelimiter.expression;

final class MemoryUtil {
    private static final Runtime runtime = Runtime.getRuntime();
    private MemoryUtil() { }
    static long availableMemory() {
        final long max = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
        return max - usedMemory(); // available memory i.e. Maximum heap size minus the bookmark amount used
    }
    static long usedMemory() {
        final long total = runtime.totalMemory(); // bookmark heap allocated to the VM process
        final long free = runtime.freeMemory(); // out of the bookmark heap, how much is free
        return total - free; // how much of the bookmark heap the VM is using
    }
    static long usedMemory(long bookmarkMemory) {
        return bookmarkMemory - availableMemory();
    }
}
