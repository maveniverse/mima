package org.cstamas.maven.mima.runtime.standalone.internal;

import com.google.inject.Guice;
import com.google.inject.Module;
import java.util.Map;
import javax.inject.Inject;
import org.apache.maven.settings.building.SettingsBuilder;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.sisu.launch.Main;
import org.eclipse.sisu.space.BeanScanning;

public class StandaloneBooter {
    @Inject
    public RepositorySystem repositorySystem;

    @Inject
    public SettingsBuilder settingsBuilder;

    public static StandaloneBooter newRepositorySystem(Map<String, String> configurationProperties) {
        final Module app = Main.wire(BeanScanning.INDEX, new StandaloneModule(configurationProperties));
        return Guice.createInjector(app).getInstance(StandaloneBooter.class);
    }
}