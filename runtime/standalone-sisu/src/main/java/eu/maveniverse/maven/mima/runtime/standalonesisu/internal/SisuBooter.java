package eu.maveniverse.maven.mima.runtime.standalonesisu.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import java.io.Closeable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.sisu.bean.LifecycleManager;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.launch.Main;
import org.eclipse.sisu.space.BeanScanning;

@Singleton
@Named
public class SisuBooter implements Closeable {
    @Inject
    public RepositorySystem repositorySystem;

    @Inject
    public SettingsBuilder settingsBuilder;

    @Inject
    public SettingsDecrypter settingsDecrypter;

    @Inject
    public LifecycleManager lifecycleManager;

    @Inject
    public MutableBeanLocator locator;

    @Override
    public void close() {
        try {
            repositorySystem.shutdown();
        } finally {
            lifecycleManager.unmanage();
            locator.clear();
        }
    }

    public static SisuBooter newSisuBooter(ContextOverrides contextOverrides) {
        final Module app = Main.wire(BeanScanning.CACHE);
        final Module overrides = new AbstractModule() {
            @Override
            protected void configure() {
                bind(ContextOverrides.class).toInstance(contextOverrides);
            }
        };
        return Guice.createInjector(app, overrides).getInstance(SisuBooter.class);
    }
}
