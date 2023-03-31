package eu.maveniverse.maven.mima.runtime.standalonesisu.internal;

import com.google.inject.Binder;
import com.google.inject.Module;
import javax.inject.Inject;
import org.eclipse.sisu.inject.MutableBeanLocator;

public class SisuModule implements Module {
    @Override
    public void configure(final Binder binder) {
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
