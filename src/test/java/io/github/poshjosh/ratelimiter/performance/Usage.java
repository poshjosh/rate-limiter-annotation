package io.github.poshjosh.ratelimiter.performance;

import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public final class Usage {

    public static Usage bookmark() {
        return of(System.currentTimeMillis(), MemoryUtil.availableMemory());
    }
    public static Usage of(long duration, long memory) {
        return new Usage(duration, memory);
    }

    private final long duration;
    private final long memory;

    public Usage(long duration, long memory) {
        this.duration = duration;
        this.memory = memory;
    }

    public void assertUsageLessThan(Usage limit) {
        Usage usage = usage();
        System.out.printf("Spent %s", DurationText.of(usage.getDuration()));
        System.out.printf(", %s", ByteText.of(usage.getMemory()));
        assertThat(usage.getDuration()).isLessThanOrEqualTo(limit.getDuration());
        assertThat(usage.getMemory()).isLessThanOrEqualTo(limit.getMemory());
    }

    public long getDuration() {
        return duration;
    }
    public long getMemory() {
        return memory;
    }

    public Usage usage() {
        return new Usage(System.currentTimeMillis() - duration, MemoryUtil.usedMemory(memory));
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
        return "Usage{duration=" + duration + ", memory=" + memory + '}';
    }

    private static final class ByteText {
        private ByteText() { }
        public static String of(long amount) {
            final String sign = amount < 0 ? "-" : "";
            amount = Math.abs(amount);
            final int oneGB = 1_000_000_000;
            if (amount >= oneGB) {
                return sign + (amount / oneGB) + " GB";
            }
            final int oneMB = oneGB / 1000;
            if (amount >= oneMB) {
                return sign + (amount / oneMB) + " MB";
            }
            final int oneKB = oneMB / 1000;
            if (amount >= oneKB) {
                return sign + (amount / oneKB) + " KB";
            }
            return  sign + amount + " bytes";
        }
    }

    private static final class DurationText {
        private DurationText() { }
        public static String of(long time) {
            final String sign = time < 0 ? "-" : "";
            time = Math.abs(time);
            final int oneHr = 60 * 60 * 1000;
            if (time >= oneHr) {
                return sign + (time / oneHr) + " hr";
            }
            final int oneMin = oneHr / 60;
            if (time >= oneMin) {
                return sign + (time / oneMin) + " min";
            }
            final int oneSec = oneMin / 60;
            if (time >= oneSec) {
                return sign + (time / oneSec) + " sec";
            }
            return  sign + time + " millis";
        }
    }
}
