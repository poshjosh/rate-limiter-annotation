package io.github.poshjosh.ratelimiter.expression;

final class MemoryUtil {
    private MemoryUtil() { }
    static long availableMemory() {
        final Runtime runtime = Runtime.getRuntime();
        final long max = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
        final long availableHeapMemory = max - _usedMemory(runtime); // available memory i.e. Maximum heap size minus the bookmark amount used
        return availableHeapMemory;
    }

    static long usedMemory() {
        return _usedMemory(Runtime.getRuntime());
    }

    private static long _usedMemory(Runtime runtime) {
        final long total = runtime.totalMemory(); // bookmark heap allocated to the VM process
        final long free = runtime.freeMemory(); // out of the bookmark heap, how much is free
        final long used = total - free; // how much of the bookmark heap the VM is using
        return used;
    }

    static long usedMemory(long bookmarkMemory) {
        return bookmarkMemory - availableMemory();
    }
}
