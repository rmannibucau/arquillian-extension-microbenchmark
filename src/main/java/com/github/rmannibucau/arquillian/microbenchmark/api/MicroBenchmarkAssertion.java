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
public @interface MicroBenchmarkAssertion {
    long total() default -1;
    long max() default -1;
    long average() default -1;
}
