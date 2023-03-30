package org.cstamas.maven.mima.runtime.sisu.internal;

import static java.util.Objects.requireNonNull;

import com.google.inject.Binder;
import com.google.inject.Module;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.wire.ParameterKeys;

public class SisuModule implements Module {
    private final Map<String, String> configurationProperties;

    public SisuModule(Map<String, String> configurationProperties) {
        this.configurationProperties = requireNonNull(configurationProperties);
    }

    @Override
    public void configure(final Binder binder) {
        binder.bind(ParameterKeys.PROPERTIES).toInstance(configurationProperties);
        binder.bind(SisuBooter.class);
        binder.bind(ShutdownThread.class).asEagerSingleton();
    }

    static final class ShutdownThread extends Thread {
        private final MutableBeanLocator locator;

        @Inject
        ShutdownThread(final MutableBeanLocator locator) {
            this.locator = locator;
            Runtime.getRuntime().addShutdownHook(this);
        }

        @Override
        public void run() {
            locator.clear();
        }
    }
}
