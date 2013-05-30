package com.github.rmannibucau.arquillian.microbenchmark.internal.client;

import com.github.rmannibucau.arquillian.microbenchmark.internal.configuration.MicroBenchmarkConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

import java.util.Map;

public class MicroBenchmarkConfigurationProducer {
    private static final String EXTENSION_NAME = "micro-benchmark";

    @Inject
    private Instance<ArquillianDescriptor> descriptor;

    @Inject
    @ApplicationScoped
    private InstanceProducer<MicroBenchmarkConfiguration> producer;

    public void loadConfiguration(final @Observes BeforeSuite event) {
        final MicroBenchmarkConfiguration configuration = new MicroBenchmarkConfiguration();

        for (final ExtensionDef def : descriptor.get().getExtensions()) {
            if (EXTENSION_NAME.equals(def.getExtensionName())) {
                final Map<String,String> extensionProperties = def.getExtensionProperties();
                configuration.readFromMap(extensionProperties);
            }
        }

        producer.set(configuration);
    }
}
