package io.github.poshjosh.ratelimiter.annotations;

public interface Rhs<V> {

    @LongCondition(that="sys.time.elapsed", is=">", rhs=1)
    V getKey();

    @LongCondition(lhs="sys.time.elapsed", op=">", rhs=1)
    V getValue();

    @SysTimeElapsed(is=">", rhs=1)
    //@SysTimeElapsed(">1")
    //@RequestUserRole("=ROLE_GUEST")
    V getSomething();
}
