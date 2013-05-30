package com.github.rmannibucau.arquillian.microbenchmark.internal.shared;

import com.github.rmannibucau.arquillian.microbenchmark.api.MicroBenchmark;
import com.github.rmannibucau.arquillian.microbenchmark.api.MicroBenchmarkAssertion;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class MicroBenchmarkRunConfiguration {
    private int threads;
    private int iterations;
    private int duration;
    private int warmupThreads;
    private int warmupDuration;
    private int warmupIterations;
    private boolean detailed;
    private boolean ignoreExceptions;

    public MicroBenchmarkRunConfiguration() {
        // no-op
    }

    public static MicroBenchmarkRunConfiguration readConfiguration(final Method method, final boolean globallyDetailed) {
        final MicroBenchmark runConfiguration = Annotations.findAnnotation(method, MicroBenchmark.class);
        if (runConfiguration == null) {
            return null;
        }

        final MicroBenchmarkRunConfiguration run = new MicroBenchmarkRunConfiguration();
        run.threads = Math.max(runConfiguration.threads(), 1);
        run.detailed = runConfiguration.mode() == MicroBenchmark.Mode.DETAILED || (runConfiguration.mode() == MicroBenchmark.Mode.UNKNWON && globallyDetailed);
        run.duration = runConfiguration.duration();
        run.iterations = runConfiguration.iterations();
        run.warmupThreads = Math.max(runConfiguration.warmupThreads(), 1);
        run.warmupDuration = runConfiguration.warmupDuration();
        run.warmupIterations = runConfiguration.warmupIterations();
        run.ignoreExceptions = runConfiguration.ignoreExceptions();

        return run;
    }

    public int getIterations() {
        return iterations;
    }

    public int getDuration() {
        return duration;
    }

    public int getThreads() {
        return threads;
    }

    public int getWarmupDuration() {
        return warmupDuration;
    }

    public int getWarmupIterations() {
        return warmupIterations;
    }

    public int getWarmupThreads() {
        return warmupThreads;
    }

    public boolean isDetailed() {
        return detailed;
    }

    public boolean isIgnoreExceptions() {
        return ignoreExceptions;
    }

    public void detailed() {
        detailed = true;
    }
}
