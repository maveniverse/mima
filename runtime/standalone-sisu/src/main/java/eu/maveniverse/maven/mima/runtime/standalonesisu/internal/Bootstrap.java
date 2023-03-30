package eu.maveniverse.maven.mima.runtime.standalonesisu.internal;

import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.mima.runtime.standalonesisu.StandaloneSisuRuntime;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.sisu.EagerSingleton;
import org.eclipse.sisu.Nullable;

@Named
@EagerSingleton
public class Bootstrap {
    @Inject
    public Bootstrap(@Nullable StandaloneSisuRuntime standaloneSisuRuntime) {
        if (standaloneSisuRuntime != null) {
            Runtimes.INSTANCE.registerRuntime(standaloneSisuRuntime);
        }
    }
}
