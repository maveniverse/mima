package org.cstamas.maven.mima.runtime.sisu.internal;

import javax.inject.Inject;
import javax.inject.Named;
import org.cstamas.maven.mima.context.Runtimes;
import org.cstamas.maven.mima.runtime.sisu.EmbeddedSisuRuntime;
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
