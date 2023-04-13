package eu.maveniverse.maven.mima.runtime.standalonesisu;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.runtime.shared.StandaloneRuntimeSupport;
import eu.maveniverse.maven.mima.runtime.standalonesisu.internal.SisuBooter;
import javax.inject.Inject;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;

public class StandaloneSisuRuntime extends StandaloneRuntimeSupport {

    private final RepositorySystem repositorySystem;

    private final SettingsBuilder settingsBuilder;

    private final SettingsDecrypter settingsDecrypter;

    public StandaloneSisuRuntime() {
        this(null, null, null);
    }

    @Inject
    public StandaloneSisuRuntime(
            RepositorySystem repositorySystem, SettingsBuilder settingsBuilder, SettingsDecrypter settingsDecrypter) {
        super("standalone-sisu", 30);
        this.repositorySystem = repositorySystem;
        this.settingsBuilder = settingsBuilder;
        this.settingsDecrypter = settingsDecrypter;
    }

    @Override
    public boolean managedRepositorySystem() {
        return repositorySystem == null;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        if (repositorySystem == null) {
            SisuBooter booter = SisuBooter.newSisuBooter();
            return buildContext(
                    this, true, overrides, booter.repositorySystem, booter.settingsBuilder, booter.settingsDecrypter);
        } else {
            return buildContext(this, false, overrides, repositorySystem, settingsBuilder, settingsDecrypter);
        }
    }
}
