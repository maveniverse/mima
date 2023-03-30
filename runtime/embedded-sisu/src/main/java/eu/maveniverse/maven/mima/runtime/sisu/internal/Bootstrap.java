package eu.maveniverse.maven.mima.runtime.sisu.internal;

import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.mima.runtime.sisu.EmbeddedSisuRuntime;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.sisu.EagerSingleton;
import org.eclipse.sisu.Nullable;

@Named
@EagerSingleton
public class Bootstrap {
    @Inject
    public Bootstrap(@Nullable EmbeddedSisuRuntime embeddedSisuRuntime) {
        if (embeddedSisuRuntime != null) {
            Runtimes.INSTANCE.registerRuntime(embeddedSisuRuntime);
        }
    }
}
