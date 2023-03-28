package org.cstamas.maven.mima.runtime.standalone.internal;

import static java.util.Objects.requireNonNull;

import com.google.inject.Binder;
import com.google.inject.Module;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.wire.ParameterKeys;

public class MimaModule implements Module {
    private final Map<String, String> configurationProperties;

    public MimaModule(Map<String, String> configurationProperties) {
        this.configurationProperties = requireNonNull(configurationProperties);
    }

    @Override
    public void configure(final Binder binder) {
        binder.bind(ParameterKeys.PROPERTIES).toInstance(configurationProperties);
        binder.bind(MimaBooter.class);
        binder.bind(ShutdownThread.class).asEagerSingleton();
    }

    static final class ShutdownThread extends Thread {
        private final ClassLoader classLoader;
        private final MutableBeanLocator locator;

        @Inject
        ShutdownThread(final MutableBeanLocator locator) {
            this.classLoader = Thread.currentThread().getContextClassLoader();
            this.locator = locator;
            Runtime.getRuntime().addShutdownHook(this);
        }

        @Override
        public void run() {
            ClassLoader original = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                locator.clear();
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    }
}
