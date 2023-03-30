package eu.maveniverse.maven.mima.runtime.standalonesisu.internal;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

public class SisuModule implements Module {
    @Override
    public void configure(final Binder binder) {
        binder.bind(SisuBooter.class);
        binder.bind(SecDispatcher.class)
                .annotatedWith(Names.named("maven"))
                .to(DefaultSecDispatcher.class)
                .in(Singleton.class);
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
