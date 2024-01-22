package io.github.poshjosh.ratelimiter.annotations;

public interface Rhs<V> {

    @LongCondition(that="system.time.elapsed", is=">", rhs=1)
    V getKey();

    @LongCondition(lhs="system.time.elapsed", op=">", rhs=1)
    V getValue();

    @SysTimeElapsed(is=">", rhs=1)
    V getSomething();
}
