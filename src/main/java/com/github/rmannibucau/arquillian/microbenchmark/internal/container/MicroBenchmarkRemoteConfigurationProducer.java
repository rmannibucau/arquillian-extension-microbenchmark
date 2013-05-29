package com.github.rmannibucau.arquillian.microbenchmark.internal.container;

import com.github.rmannibucau.arquillian.microbenchmark.internal.configuration.MicroBenchmarkConfiguration;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

import java.io.InputStream;

public class MicroBenchmarkRemoteConfigurationProducer {
    @Inject
    @ApplicationScoped
    private InstanceProducer<MicroBenchmarkConfiguration> configuration;

    public void loadConfiguration(final @Observes BeforeSuite event) {
        final MicroBenchmarkConfiguration config = new MicroBenchmarkConfiguration();
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(MicroBenchmarkConfiguration.CONFIGURATION_PATH);
        if (is != null) {
            config.initFromInputStream(is);
        }
        configuration.set(config);

    }
}
