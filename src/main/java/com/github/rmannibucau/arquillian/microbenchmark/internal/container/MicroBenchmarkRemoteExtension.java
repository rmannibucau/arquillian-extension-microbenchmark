package com.github.rmannibucau.arquillian.microbenchmark.internal.container;

import com.github.rmannibucau.arquillian.microbenchmark.internal.shared.MicroBenchmarkObserver;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;

public class MicroBenchmarkRemoteExtension implements RemoteLoadableExtension {
    @Override
    public void register(final ExtensionBuilder builder) {
        builder.observer(MicroBenchmarkObserver.class)
            .observer(MicroBenchmarkRemoteConfigurationProducer.class);
    }
}
