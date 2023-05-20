package io.github.poshjosh.ratelimiter.performance.dummyclasses.dummyclasses0;

import io.github.poshjosh.ratelimiter.annotations.Rate;
import io.github.poshjosh.ratelimiter.annotations.RateCondition;
import io.github.poshjosh.ratelimiter.performance.dummyclasses.RateGroupA;

import java.time.Instant;

@RateGroupA
public class RateLimitedClass0 {
    public static final String METHOD_5_KEY = "..performance.dummyclasses.dummyclasses0.RateLimitedClass0#method5";
    public static final int METHOD_5_LIMIT = 5;
    void method_0() {}
    void $method_1(String sval, Instant instant) {}
    String _method_2(Long lval, Object obj) {
        return "method_2";
    }
    private void method_3(boolean bval) {}
    @Rate(10)
    @RateCondition("sys.time.elapsed>PT3S")
    public String methodNumber4(Object a, Object b, Object c, Object d, Object e, Object f,
            Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n,
            Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v,
            Object w, Object x, Object y, Object z) {
        String id =  this.getClass().getName() + "methodNumber4";
        System.out.println(id);
        return id;
    }

    @Rate(name = METHOD_5_KEY, permits = METHOD_5_LIMIT)
    public String methodNumber5(String a) {
        String id =  this.getClass().getName() + "methodNumber5";
        System.out.println(id);
        return id;
    }

    private void method_4() {}
    
    private void method_5() {}
    
    private void method_6() {}
    
    private void method_7() {}
    
    private void method_8() {}
    
    private void method_9() {}
}
