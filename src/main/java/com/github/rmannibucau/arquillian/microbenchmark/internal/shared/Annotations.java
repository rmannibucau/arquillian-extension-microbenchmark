package com.github.rmannibucau.arquillian.microbenchmark.internal.shared;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class Annotations {
    public static <T extends Annotation> T findAnnotation(final Method method, final Class<T> annotation) {
        T runConfiguration = method.getAnnotation(annotation);
        if (runConfiguration != null) {
            return runConfiguration;
        }
        return method.getDeclaringClass().getAnnotation(annotation);
    }

    private Annotations() {
        // no-op
    }
}
