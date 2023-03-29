package org.cstamas.maven.mima.engines.maven.internal;

import javax.inject.Inject;
import javax.inject.Named;
import org.codehaus.plexus.component.annotations.Component;
import org.cstamas.maven.mima.core.engine.Engines;
import org.cstamas.maven.mima.engines.maven.MavenEngine;
import org.eclipse.sisu.EagerSingleton;
import org.eclipse.sisu.Nullable;

@Named
@EagerSingleton
@Component(role = Bootstrap.class, instantiationStrategy = "load-on-start")
public class Bootstrap {
    @Inject
    public Bootstrap(@Nullable MavenEngine mavenEngine) {
        if (mavenEngine != null) {
            Engines.INSTANCE.registerEngine(mavenEngine);
        }
    }
}
