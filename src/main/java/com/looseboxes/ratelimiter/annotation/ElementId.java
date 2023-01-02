package com.looseboxes.ratelimiter.annotation;

import java.lang.reflect.Method;

public final class ElementId {
    private ElementId() { }
    public static String of(Class<?> clazz) {
        return clazz.getName();
    }

    /**
     * Identify a method.
     *
     * Given a class with 2 methods
     *
     * <pre>
     *     package com.example;
     *     class ExampleClass{
     *         void methodA() { }
     *         protected String methodB(Long key, String value) { return value; }
     *     }
     * </pre>
     *
     * <p>For <pre>methodA</pre> will return <pre>void com.example.ExampleClass.methodA()</pre></p>
     *
     * <p>
     *     For <pre>methodB</pre> will return <pre>com.example.ExampleClass.methodB(java.lang.Long,java.lang.String)</pre>
     * </p>
     *
     * @param method The method whose ID is to be returned
     * @return An identifier for the specified method
     */
    public static String of(Method method) {
        final String methodString = method.toString();
        final int indexOfClassName = methodString.indexOf(method.getDeclaringClass().getName());
        if(indexOfClassName == -1) {
            throw new AssertionError("Method#toString() does not contain the method's declaring class name as expected. Method: " + method);
        }
        return methodString.substring(indexOfClassName);
    }
}
