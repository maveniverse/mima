package eu.maveniverse.maven.mima.runtime.standalonesisu.internal;

import com.google.inject.Guice;
import com.google.inject.Module;
import javax.inject.Inject;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.sisu.launch.Main;
import org.eclipse.sisu.space.BeanScanning;

public class SisuBooter {
    @Inject
    public RepositorySystem repositorySystem;

    @Inject
    public SettingsBuilder settingsBuilder;

    @Inject
    public SettingsDecrypter settingsDecrypter;

    public static SisuBooter newRepositorySystem() {
        final Module app = Main.wire(BeanScanning.INDEX, new SisuModule());
        return Guice.createInjector(app).getInstance(SisuBooter.class);
    }
}
