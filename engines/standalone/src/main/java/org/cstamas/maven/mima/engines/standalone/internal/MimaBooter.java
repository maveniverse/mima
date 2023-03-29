package org.cstamas.maven.mima.engines.standalone.internal;

import com.google.inject.Guice;
import com.google.inject.Module;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.sisu.launch.Main;
import org.eclipse.sisu.space.BeanScanning;

@Named
public class MimaBooter {
    @Inject
    public RepositorySystem repositorySystem;

    public static MimaBooter newRepositorySystem(Map<String, String> configurationProperties) {
        final Module app = Main.wire(BeanScanning.INDEX, new MimaModule(configurationProperties));
        return Guice.createInjector(app).getInstance(MimaBooter.class);
    }
}
