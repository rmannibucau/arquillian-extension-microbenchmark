package com.github.rmannibucau.arquillian.microbenchmark.internal.shared;

import com.github.rmannibucau.arquillian.microbenchmark.api.AssertionException;
import com.github.rmannibucau.arquillian.microbenchmark.api.MicroBenchmarkAssertion;
import com.github.rmannibucau.arquillian.microbenchmark.internal.configuration.MicroBenchmarkConfiguration;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jboss.arquillian.container.test.impl.execution.LocalTestExecuter;
import org.jboss.arquillian.container.test.impl.execution.event.LocalExecutionEvent;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.annotation.TestScoped;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MicroBenchmarkObserver extends LocalTestExecuter {
    private static final Logger LOGGER = Logger.getLogger(MicroBenchmarkObserver.class.getName());

    private static final long DEFAULT_TIMEOUT_MS = 600000;

    @Inject
    private Instance<MicroBenchmarkConfiguration> configuration;

    @Inject
    @TestScoped
    private InstanceProducer<TestResult> testResult;

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    @Override
    public void execute(final @Observes LocalExecutionEvent event) throws Exception {
        // no-op
    }

    public void bench(final @Observes(precedence = -1) EventContext<LocalExecutionEvent> eventContext) throws Throwable {
        final TestMethodExecutor executor = eventContext.getEvent().getExecutor();
        final MicroBenchmarkRunConfiguration run = MicroBenchmarkRunConfiguration.readConfiguration(executor.getMethod());
        if (run == null) {
            eventContext.proceed();
            return;
        }

        final TestResult result = new TestResult();
        try {
            final Object[] parameters = findParameters(executor, serviceLoader.get().all(TestEnricher.class));

            warmup(executor, run, parameters);
            bench(executor, parameters, run);

            result.setStatus(TestResult.Status.PASSED);
        } catch (final Throwable e) {
            result.setStatus(TestResult.Status.FAILED);
            result.setThrowable(e);

            reportException(e);
        } finally {
            result.setEnd(System.currentTimeMillis());
        }
        testResult.set(result);
    }

    // warmup is mono threaded
    private void warmup(final TestMethodExecutor executor, final MicroBenchmarkRunConfiguration run, final Object[] parameters) throws Throwable {
        if (run.getWarmupDuration() > 0) {
            final ExecutorService es = Executors.newFixedThreadPool(run.getWarmupThreads());
            LOGGER.info("Running warmup for " + run.getWarmupDuration() + "ms");

            final Collection<TimedInvocation> timedTasks = new ArrayList<TimedInvocation>();
            final long start = System.nanoTime();
            final long end = start + TimeUnit.MILLISECONDS.toNanos(run.getDuration());

            for (int i = 0; i < run.getWarmupThreads(); i++) {
                timedTasks.add(new TimedInvocation(executor, parameters, end, run.isDetailed()));
            }

            es.invokeAll(timedTasks);
            es.shutdown();
            waitExecutor(es, extractTimeout(executor.getMethod()), TimeUnit.MILLISECONDS);
        }

        if (run.getWarmupIterations() > 0) {
            final ExecutorService es = Executors.newFixedThreadPool(run.getWarmupThreads());
            LOGGER.info("Running warmup for " + run.getWarmupIterations() + " iterations");

            for (int i = 0; i < run.getWarmupIterations(); i++) {
                es.submit(new Invocation(executor, parameters));
            }

            es.shutdown();
            waitExecutor(es, extractTimeout(executor.getMethod()), TimeUnit.MILLISECONDS);
        }
    }

    private void bench(final TestMethodExecutor executor, final Object[] params, final MicroBenchmarkRunConfiguration run) throws Throwable {
        final int threads = run.getThreads();

        final long timeout = extractTimeout(executor.getMethod());
        if (run.getDuration() > 0) {
            final ExecutorService es = Executors.newFixedThreadPool(threads);

            LOGGER.info("Running micro-bench for " + run.getDuration() + " ms");

            final Collection<TimedInvocation> timedTasks = new ArrayList<TimedInvocation>();
            { // create time limited threads
                final long start = System.nanoTime();
                final long end = start + TimeUnit.MILLISECONDS.toNanos(run.getDuration());
                for (int i = 0; i < threads; i++) {
                    timedTasks.add(new TimedInvocation(executor, params, end, run.isDetailed()));
                }
            }

            final long realStart = System.nanoTime();
            es.invokeAll(timedTasks);
            waitExecutor(es, timeout, TimeUnit.MILLISECONDS);
            final long realEnd = System.nanoTime();

            // aggregate results
            final List<Throwable> results = extractErrors(timedTasks);

            final List<Long> durations = new ArrayList<Long>();
            for (final TimedInvocation invocation : timedTasks) {
                durations.addAll(invocation.getDurations());
            }


            // report
            final long duration = TimeUnit.NANOSECONDS.toMillis(realEnd - realStart);
            validExecution(results, run.isIgnoreExceptions());
            final DescriptiveStatistics stats = report(executor, run, durations, duration);
            asserts(executor, stats, duration);
        }

        if (run.getIterations() > 0) {
            final Collection<Invocation> tasks = createTaks(executor, params, run);
            LOGGER.info("Running micro-bench for " + run.getIterations() + " iterations");

            // wait all task are done and measure the duration
            final ExecutorService es = Executors.newFixedThreadPool(threads);
            final long start = System.nanoTime();
            final List<Future<Throwable>> results = es.invokeAll(tasks);
            waitExecutor(es, timeout, TimeUnit.MILLISECONDS);
            final long end = System.nanoTime();

            // aggregate durations
            final List<Long> durations = new ArrayList<Long>();
            if (run.isDetailed()) {
                for (final Invocation i : tasks) {
                    durations.add(DetailedInvocation.class.cast(i).getDuration());
                }
            }

            // report
            final long duration = TimeUnit.NANOSECONDS.toMillis(end - start);
            validExecution(unwrapFutures(results), run.isIgnoreExceptions());
            final DescriptiveStatistics stats = report(executor, run, durations, duration);
            asserts(executor, stats, duration);
        }
    }

    private List<Throwable> extractErrors(Collection<TimedInvocation> timedTasks) {
        final List<Throwable> results = new ArrayList<Throwable>();
        for (final TimedInvocation invocation : timedTasks) {
            results.addAll(invocation.getResults());
        }
        return results;
    }

    private static void asserts(final TestMethodExecutor executor, final DescriptiveStatistics stats, final long total) {
        final MicroBenchmarkAssertion assertions = Annotations.findAnnotation(executor.getMethod(), MicroBenchmarkAssertion.class);
        if (assertions == null) {
            return;
        }

        checkAssertion("max", assertions.max(), stats.getMax());
        checkAssertion("average", assertions.average(), stats.getMean());
        checkAssertion("total", assertions.total(), total);
    }

    private static void checkAssertion(final String msg, final long expectedMax, final double actualMax) {
        if (expectedMax >= 0) { // else no config, no assertion
            if (actualMax > expectedMax) {
                throw new AssertionException("Expected " + msg + " < " + expectedMax + " and got " + actualMax);
            }
        }
    }

    private DescriptiveStatistics report(final TestMethodExecutor executor, final MicroBenchmarkRunConfiguration run, final Collection<Long> durations, long duration) {
        LOGGER.info("Micro-bench on " + executor.getMethod().getDeclaringClass().getSimpleName() + "#"
                + executor.getMethod().getName() + " lasted " + duration + "ms with " + run.getThreads() + " threads.");

        final DescriptiveStatistics stats = createStatistics(durations);
        if (run.isDetailed()) {
            // log stats
            final String info = stats.toString().replace("\n", ", ").substring("DescriptiveStatistics:, ".length());
            LOGGER.info("Aggregates: " + info);

            if (configuration.get().isSaveDiagram()) {
                saveDiagram(executor, durations, configuration.get(), info);
            }

            return stats;
        }

        // when not detailed the assertions valid only duration
        stats.addValue(duration);
        return stats;
    }

    private static DescriptiveStatistics createStatistics(final Collection<Long> durations) {
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final Long it : durations) {
            stats.addValue(it);
        }
        return stats;
    }

    private static void saveDiagram(final TestMethodExecutor executor, final Collection<Long> durations, final MicroBenchmarkConfiguration config, final String info) {
        final Map<Long, Integer> xyValues = new HashMap<Long, Integer>();
        for (final Long it : durations) {
            final Integer number = xyValues.get(it);
            if (number == null) {
                xyValues.put(it, 1);
            } else {
                xyValues.put(it, 1 + number);
            }
        }

        final XYSeries xy = new XYSeries(1, true);
        for (final Map.Entry<Long, Integer> entry : xyValues.entrySet()) {
            xy.add(entry.getKey(), entry.getValue());
        }
        final JFreeChart chart = ChartFactory.createHistogram("Method durations (" + info + ")", "Duration (ms)", "Number", new XYSeriesCollection(xy), PlotOrientation.VERTICAL, true, true, false);
        chart.getXYPlot().getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        chart.getXYPlot().getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        try {
            final File file = new File(config.getDiagramFolder(), executor.getMethod().getName() + ".png");
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                LOGGER.warning("Can't create " + file.getParent());
            }
            ChartUtilities.saveChartAsPNG(file, chart, config.getWidth(), config.getHeight());
        } catch (final IOException e) {
            // no-op: swallowing
        }
    }

    private static Collection<Invocation> createTaks(final TestMethodExecutor executor, final Object[] params, final MicroBenchmarkRunConfiguration run) {
        final Collection<Invocation> tasks = new ArrayList<Invocation>(run.getIterations());
        for (int i = 0; i < run.getIterations(); i++) {
            tasks.add(newInvocation(executor, params, run.isDetailed()));
        }
        return tasks;
    }

    private static Invocation newInvocation(final TestMethodExecutor executor, final Object[] params, final boolean detailed) {
        final Invocation invocation;
        if (!detailed) {
            invocation = new Invocation(executor, params);
        } else {
            invocation = new DetailedInvocation(executor, params);
        }
        return invocation;
    }

    // this method is just a hack to avoid to redefine the parameter building logic
    private Object[] findParameters(final TestMethodExecutor executor, final Collection<TestEnricher> all) {
        try {
            final Method m = LocalTestExecuter.class.getDeclaredMethod("enrichArguments", Method.class, Collection.class);
            m.setAccessible(true);
            return Object[].class.cast(m.invoke(this, executor.getMethod(), all));
        } catch (final Exception e) {
            throw new RuntimeException("Arquillian internals have probably changed, check your version", e);
        }
    }

    // arquillian junit rely on State class to store the exception, done by reflection to not be linked to junit
    private static void reportException(final Throwable e) {
        try {
            final Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass("org.jboss.arquillian.junit.State");
            clazz.getDeclaredMethod("caughtTestException", Throwable.class).invoke(null, e);
        } catch (final Exception ignored) {
            // no-op
        } catch (final NoClassDefFoundError ignored) {
            // no-op
        }
    }

    private static void validExecution(final Collection<Throwable> throwables, final boolean ignoreExceptions) throws Exception {
        if (!throwables.isEmpty() && !ignoreExceptions) {
            throw new MultipleExceptions(throwables);
        }
    }

    private static void waitExecutor(final ExecutorService es, final long duration, final TimeUnit unit) throws InterruptedException {
        es.shutdown();
        es.awaitTermination(duration, unit);
    }

    private static long extractTimeout(final Method method) {
        for (final Annotation a : method.getDeclaredAnnotations()) {
            if (a.annotationType().getName().equals("org.junit.Test")) {
                try {
                    final Long timeout = (Long) a.annotationType().getMethod("timeout").invoke(a);
                    if (timeout == 0) {
                        return DEFAULT_TIMEOUT_MS;
                    }
                    return timeout;
                } catch (final Exception e) {
                    // no-op
                }
            }
        }
        return DEFAULT_TIMEOUT_MS;
    }

    private static <T> Collection<T> unwrapFutures(final Collection<Future<T>> futures) throws InterruptedException, ExecutionException {
        final Collection<T> list = new ArrayList<T>();
        for (final Future<T> result : futures) {
            final T value = result.get();
            if (value != null) {
                list.add(value);
            }
        }
        return list;
    }

    private static class Invocation implements Callable<Throwable> {
        private final TestMethodExecutor executor;
        private final Object[] parameters;

        public Invocation(final TestMethodExecutor executor, final Object[] params) {
            this.executor = executor;
            this.parameters = params;
        }

        @Override
        public Throwable call() throws Exception {
            try {
                executor.invoke(parameters);
                return null;
            } catch (final Throwable throwable) {
                return throwable;
            }
        }
    }

    private static class TimedInvocation extends Invocation {
        private final TestMethodExecutor executor;
        private final Object[] parameters;
        private final long end;
        private final boolean detailed;

        private final Collection<Throwable> results = new ArrayList<Throwable>();
        private final Collection<Long> durations = new ArrayList<Long>();

        public TimedInvocation(final TestMethodExecutor executor, final Object[] params, final long end, final boolean detailed) {
            super(executor, params);
            this.executor = executor;
            this.parameters = params;
            this.end = end;
            this.detailed = detailed;
        }

        @Override
        public Throwable call() throws Exception {
            long start = 0;

            while ((System.nanoTime() - end) < 0) {
                if (detailed) {
                    start = System.nanoTime();
                }
                try {
                    final Throwable exception = super.call();
                    if (exception != null) {
                        results.add(exception);
                    }
                } finally {
                    if (detailed) {
                        durations.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                    }
                }
            }

            if (!results.isEmpty()) {
                return new MultipleExceptions(results);
            }

            return null;
        }

        private Collection<Long> getDurations() {
            return durations;
        }

        private Collection<Throwable> getResults() {
            return results;
        }
    }

    private static class DetailedInvocation extends Invocation {
        private long duration;

        public DetailedInvocation(final TestMethodExecutor executor, final Object[] params) {
            super(executor, params);
        }

        @Override
        public Throwable call() throws Exception {
            duration = System.nanoTime();
            try {
                return super.call();
            } finally {
                duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - duration);
            }
        }

        public long getDuration() {
            return duration;
        }
    }
}
