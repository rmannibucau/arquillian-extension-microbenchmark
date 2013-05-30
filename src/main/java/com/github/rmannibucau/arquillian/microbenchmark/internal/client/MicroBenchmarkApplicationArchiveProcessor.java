package com.github.rmannibucau.arquillian.microbenchmark.internal.client;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.ziplock.JarLocation;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.filter.IncludeRegExpPaths;
import org.jfree.JCommon;
import org.jfree.chart.JFreeChart;

public class MicroBenchmarkApplicationArchiveProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(final Archive<?> applicationArchive, final TestClass testClass) {
        if (LibraryContainer.class.isInstance(applicationArchive) && !JavaArchive.class.isInstance(applicationArchive)) {
            final LibraryContainer container = LibraryContainer.class.cast(applicationArchive);

            if (applicationArchive.getContent(new IncludeRegExpPaths("/WEB-INF/lib/commons-math3.*.jar")).size() == 0) {
                container.addAsLibraries(JarLocation.jarLocation(DescriptiveStatistics.class));
            }
            if (applicationArchive.getContent(new IncludeRegExpPaths("/WEB-INF/lib/jfreechart.*.jar")).size() == 0) {
                container.addAsLibraries(JarLocation.jarLocation(JCommon.class), JarLocation.jarLocation(JFreeChart.class));
            }
        }
    }
}
