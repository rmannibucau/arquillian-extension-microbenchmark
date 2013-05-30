package com.github.rmannibucau.arquillian.microbenchmark.api;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface MicroBenchmark {
    int threads() default 1;

    int iterations() default 1;
    int duration() default -1; // ms

    int warmupIterations() default -1;
    int warmupDuration() default -1; // ms
    int warmupThreads() default 1;

    boolean ignoreExceptions() default false;

    Mode mode() default Mode.UNKNWON;

    static enum  Mode {
        NONE, DETAILED, UNKNWON;
    }
}
