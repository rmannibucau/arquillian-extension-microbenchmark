package com.github.rmannibucau.arquillian.microbenchmark.internal.client;

import com.github.rmannibucau.arquillian.microbenchmark.internal.container.MicroBenchmarkRemoteExtension;
import com.github.rmannibucau.arquillian.microbenchmark.internal.shared.MicroBenchmarkObserver;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class MicroBenchmarkExtension implements LoadableExtension {
    @Override
    public void register(final ExtensionBuilder builder) {
        builder.service(AuxiliaryArchiveAppender.class, MicroBenchmarkArchiveAppender.class)
            .service(ApplicationArchiveProcessor.class, MicroBenchmarkApplicationArchiveProcessor.class)
            .service(RemoteLoadableExtension.class, MicroBenchmarkRemoteExtension.class)
            .observer(MicroBenchmarkConfigurationProducer.class)
            .observer(MicroBenchmarkObserver.class);
    }
}
