package io.github.poshjosh.ratelimiter.performance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Objects;

public final class Usage {

    public static Usage bookmark() {
        return of(System.currentTimeMillis(), availableMemory());
    }
    public static Usage of(long duration, long memory) {
        return new Usage(duration, memory);
    }

    private final long duration;
    private final long memory;

    private Usage(long duration, long memory) {
        this.duration = duration;
        this.memory = memory;
    }

    public Usage current() {
        return new Usage(System.currentTimeMillis() - duration, usedMemory(memory));
    }

    public boolean isAnyUsageGreaterThan(Usage other) {
        return duration > other.getDuration() || memory > other.getMemory();
    }

    public long getDuration() {
        return duration;
    }
    public long getMemory() {
        return memory;
    }

    private static final Runtime runtime = Runtime.getRuntime();
    private static long usedMemory(long bookmarkMemory) {
        return bookmarkMemory - availableMemory();
    }
    private static long availableMemory() {
        final long max = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
        return max - usedMemory(); // available memory i.e. Maximum heap size minus the bookmark amount used
    }
    private static long usedMemory() {
        final long total = runtime.totalMemory(); // bookmark heap allocated to the VM process
        final long free = runtime.freeMemory(); // out of the bookmark heap, how much is free
        return total - free; // how much of the bookmark heap the VM is using
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Usage that = (Usage) o;
        return duration == that.duration && memory == that.memory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, memory);
    }

    @Override
    public String toString() {
        return "Usage{duration=" + Duration.ofMillis(duration) + ", memory=" + ByteText.of(memory) + '}';
    }

    private static final class ByteText {
        private ByteText() { }
        public static String of(long amount) {
            return of(amount, 3);
        }
        public static String of(long amount, int scale) {
            amount = Math.abs(amount);
            int divisor = 1_000_000_000;
            if (amount >= divisor) {
                return print(amount, divisor, scale);
            }
            divisor = divisor / 1000;
            if (amount >= divisor) {
                return print(amount, divisor, scale);
            }
            divisor = divisor / 1000;
            if (amount >= divisor) {
                return print(amount, divisor, scale);
            }
            return  print(amount, 1, scale);
        }
    }

    private static String print(long dividend, int divisor, int scale) {
        BigDecimal value = divide(dividend, divisor, scale);
        return (dividend < 0 ? "-" : "") + value + getSymbol(divisor);
    }

    private static BigDecimal divide(long dividend, int divisor, int scale) {
        if (dividend == 0) {
            return BigDecimal.ZERO;
        }
        if (divisor == 1) {
            return BigDecimal.valueOf(dividend).setScale(scale, RoundingMode.CEILING);
        }
        return BigDecimal.valueOf(dividend).divide(BigDecimal.valueOf(divisor))
                .setScale(scale, RoundingMode.CEILING);
    }

    private static String getSymbol(int factor) {
        switch (factor) {
            case 1_000_000_000: return "GB";
            case 1_000_000: return "MB";
            case 1_000: return "KB";
            default: return "B";
        }
    }
}
