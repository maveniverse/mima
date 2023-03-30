package org.cstamas.maven.mima.runtime.standalonesisu.internal;

import com.google.inject.Guice;
import com.google.inject.Module;
import java.util.Map;
import javax.inject.Inject;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.settings.building.SettingsBuilder;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.sisu.launch.Main;
import org.eclipse.sisu.space.BeanScanning;

public class SisuBooter {
    @Inject
    public RepositorySystem repositorySystem;

    @Inject
    public ModelBuilder modelBuilder;

    @Inject
    public SettingsBuilder settingsBuilder;

    public static SisuBooter newRepositorySystem(Map<String, String> configurationProperties) {
        final Module app = Main.wire(BeanScanning.INDEX, new SisuModule(configurationProperties));
        return Guice.createInjector(app).getInstance(SisuBooter.class);
    }
}
