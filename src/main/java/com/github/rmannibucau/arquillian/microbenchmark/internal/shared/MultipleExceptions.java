package com.github.rmannibucau.arquillian.microbenchmark.internal.shared;

import java.util.Collection;

public class MultipleExceptions extends RuntimeException {
    private final Collection<Throwable> errors;

    public MultipleExceptions(final Collection<Throwable> throwables) {
        this.errors = throwables;
    }

    public Collection<Throwable> getErrors() {
        return errors;
    }
}
