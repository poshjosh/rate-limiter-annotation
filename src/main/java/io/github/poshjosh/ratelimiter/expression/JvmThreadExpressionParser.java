package io.github.poshjosh.ratelimiter.expression;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;

final class JvmThreadExpressionParser<S> implements ExpressionParser<S, Object> {

    public static final String COUNT = "jvm.thread.count";
    public static final String COUNT_DAEMON = "jvm.thread.count.daemon";
    public static final String COUNT_DEADLOCKED = "jvm.thread.count.deadlocked";
    public static final String COUNT_DEADLOCKED_MONITOR = "jvm.thread.count.deadlocked.monitor";
    public static final String COUNT_PEAK = "jvm.thread.count.peak";
    public static final String COUNT_STARTED = "jvm.thread.count.started";
    public static final String CURRENT_COUNT_BLOCKED = "jvm.thread.current.count.blocked";
    public static final String CURRENT_COUNT_WAITED = "jvm.thread.current.count.waited";
    public static final String CURRENT_STATE = "jvm.thread.current.state";
    public static final String CURRENT_SUSPENDED = "jvm.thread.current.suspended";
    public static final String CURRENT_TIME_BLOCKED = "jvm.thread.current.time.blocked";
    public static final String CURRENT_TIME_CPU = "jvm.thread.current.time.cpu";
    public static final String CURRENT_TIME_USER = "jvm.thread.current.time.user";
    public static final String CURRENT_TIME_WAITED = "jvm.thread.current.time.waited";

    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    JvmThreadExpressionParser() { }

    @Override
    public boolean isSupported(Expression<String> expression) {
        final String lhs = expression.requireLeft();
        switch (lhs) {
            case COUNT:
            case COUNT_DAEMON:
            case COUNT_DEADLOCKED:
            case COUNT_DEADLOCKED_MONITOR:
            case COUNT_PEAK:
            case COUNT_STARTED:
            case CURRENT_COUNT_BLOCKED:
            case CURRENT_COUNT_WAITED:
            case CURRENT_TIME_BLOCKED:
            case CURRENT_TIME_CPU:
            case CURRENT_TIME_USER:
            case CURRENT_TIME_WAITED:
                return expression.getOperator().isType(Operator.Type.COMPARISON);
            case CURRENT_STATE:
            case CURRENT_SUSPENDED:
                return expression.getOperator().equalsIgnoreNegation(Operator.EQUALS);
            default:
                return false;
        }
    }

    @Override
    public Expression<Object> parse(S source, Expression<String> expression) {
        final String lhs = expression.requireLeft();
        final Object right = right(expression);
        switch (lhs) {
            case COUNT:
                return expression.with((long)threadMXBean.getThreadCount(), right);
            case COUNT_DAEMON:
                return expression.with((long)threadMXBean.getDaemonThreadCount(), right);
            case COUNT_DEADLOCKED:
                return expression.with(length(threadMXBean.findDeadlockedThreads()), right);
            case COUNT_DEADLOCKED_MONITOR:
                return expression.with(length(threadMXBean.findMonitorDeadlockedThreads()), right);
            case COUNT_PEAK:
                return expression.with((long)threadMXBean.getPeakThreadCount(), right);
            case COUNT_STARTED:
                return expression.with(threadMXBean.getTotalStartedThreadCount(), right);
            case CURRENT_COUNT_BLOCKED:
                return expression.with(threadInfo().getBlockedCount(), right);
            case CURRENT_COUNT_WAITED:
                return expression.with(threadInfo().getWaitedCount(), right);
            case CURRENT_STATE:
                return expression.with(threadInfo().getThreadState(), right);
            case CURRENT_SUSPENDED:
                return expression.with(threadInfo().isSuspended(), right);
            case CURRENT_TIME_BLOCKED:
                return expression.with(threadInfo().getBlockedTime(), right);
            case CURRENT_TIME_CPU:
                return expression.with(threadMXBean.getCurrentThreadCpuTime(), right);
            case CURRENT_TIME_USER:
                return expression.with(threadMXBean.getCurrentThreadUserTime(), right);
            case CURRENT_TIME_WAITED:
                return expression.with(threadInfo().getWaitedTime(), right);
            default:
                throw Checks.notSupported(this, lhs);
        }
    }

    private long length(long [] array) {
        return array == null ? 0 : array.length;
    }

    private ThreadInfo threadInfo() {
        return threadMXBean.getThreadInfo(Thread.currentThread().getId());
    }

    private Object right(Expression<String> expression) {
        final String lhs = expression.requireLeft();
        switch (lhs) {
            case COUNT:
            case COUNT_DAEMON:
            case COUNT_DEADLOCKED:
            case COUNT_DEADLOCKED_MONITOR:
            case COUNT_PEAK:
            case COUNT_STARTED:
            case CURRENT_COUNT_BLOCKED:
            case CURRENT_COUNT_WAITED:
                return Long.parseLong(expression.requireRight());
            case CURRENT_STATE:
                return Thread.State.valueOf(expression.requireRight());
            case CURRENT_SUSPENDED:
                return Boolean.parseBoolean(expression.requireRight());
            case CURRENT_TIME_BLOCKED:
            case CURRENT_TIME_CPU:
            case CURRENT_TIME_USER:
            case CURRENT_TIME_WAITED:
                return Duration.parse(expression.requireRight()).toMillis();
            default:
                throw Checks.notSupported(this, lhs);
        }
    }
}
