package org.cstamas.maven.mima.engines.maven.internal;

import javax.inject.Inject;
import javax.inject.Named;
import org.codehaus.plexus.component.annotations.Component;
import org.cstamas.maven.mima.core.MimaEngines;
import org.cstamas.maven.mima.engines.maven.MavenMimaEngine;
import org.eclipse.sisu.EagerSingleton;
import org.eclipse.sisu.Nullable;

@Named
@EagerSingleton
@Component(role = Bootstrap.class, instantiationStrategy = "load-on-start")
public class Bootstrap {
    @Inject
    public Bootstrap(@Nullable MavenMimaEngine mavenEngine) {
        if (mavenEngine != null) {
            MimaEngines.INSTANCE.registerEngine(mavenEngine);
        }
    }
}
