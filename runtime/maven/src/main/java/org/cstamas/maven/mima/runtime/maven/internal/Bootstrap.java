package org.cstamas.maven.mima.runtime.maven.internal;

import javax.inject.Inject;
import javax.inject.Named;
import org.codehaus.plexus.component.annotations.Component;
import org.cstamas.maven.mima.context.Runtimes;
import org.cstamas.maven.mima.runtime.maven.MavenRuntime;
import org.eclipse.sisu.EagerSingleton;
import org.eclipse.sisu.Nullable;

@Named
@EagerSingleton
@Component(role = Bootstrap.class, instantiationStrategy = "load-on-start")
public class Bootstrap {
    @Inject
    public Bootstrap(@Nullable MavenRuntime mavenEngine) {
        if (mavenEngine != null) {
            Runtimes.INSTANCE.registerRuntime(mavenEngine);
        }
    }
}
