package com.github.rmannibucau.arquillian.microbenchmark.internal.client;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.ziplock.JarLocation;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jfree.JCommon;
import org.jfree.chart.JFreeChart;

public class MicroBenchmarkApplicationArchiveProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(final Archive<?> applicationArchive, final TestClass testClass) {
        if (LibraryContainer.class.isInstance(applicationArchive) && !JavaArchive.class.isInstance(applicationArchive)) {
            final LibraryContainer container = LibraryContainer.class.cast(applicationArchive);
            container.addAsLibraries(
                    JarLocation.jarLocation(DescriptiveStatistics.class),
                    JarLocation.jarLocation(JCommon.class),
                    JarLocation.jarLocation(JFreeChart.class));
        }
    }
}
