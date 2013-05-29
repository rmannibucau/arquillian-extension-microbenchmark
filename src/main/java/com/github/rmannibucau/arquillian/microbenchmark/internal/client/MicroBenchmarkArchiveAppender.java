package com.github.rmannibucau.arquillian.microbenchmark.internal.client;

import com.github.rmannibucau.arquillian.microbenchmark.api.MicroBenchmark;
import com.github.rmannibucau.arquillian.microbenchmark.internal.configuration.MicroBenchmarkConfiguration;
import com.github.rmannibucau.arquillian.microbenchmark.internal.container.MicroBenchmarkRemoteExtension;
import com.github.rmannibucau.arquillian.microbenchmark.internal.shared.MicroBenchmarkObserver;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class MicroBenchmarkArchiveAppender extends CachedAuxilliaryArchiveAppender{
    @Inject
    private Instance<MicroBenchmarkConfiguration> configuration;

    @Override
    public Archive<?> buildArchive() {
        return ShrinkWrap.create(JavaArchive.class, "micro-benchmark.jar")
                    .addAsServiceProvider(RemoteLoadableExtension.class, MicroBenchmarkRemoteExtension.class)
                    .addPackage(MicroBenchmark.class.getPackage())
                    .addPackage(MicroBenchmarkConfiguration.class.getPackage())
                    .addPackage(MicroBenchmarkObserver.class.getPackage())
                    .addPackage(MicroBenchmarkRemoteExtension.class.getPackage())
                    .addAsResource(new StringAsset(configuration.get().asString()), MicroBenchmarkConfiguration.CONFIGURATION_PATH);
    }
}
