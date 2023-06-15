package eu.maveniverse.maven.mima.runtime.standalonestatic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.runtime.shared.StandaloneRuntimeSupport;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;

public class StandaloneStaticRuntime extends StandaloneRuntimeSupport {

    public StandaloneStaticRuntime() {
        this("standalone-static", 40);
    }

    public StandaloneStaticRuntime(String name, int priority) {
        super(name, priority);
    }

    @Override
    public boolean managedRepositorySystem() {
        return true;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        RepositorySystem repositorySystem = requireNonNull(createRepositorySystem(overrides));
        SettingsBuilder settingsBuilder = requireNonNull(createSettingsBuilder(overrides));
        SettingsDecrypter settingsDecrypter = requireNonNull(createSettingsDecrypter(overrides));
        ProfileSelector profileSelector = requireNonNull(createProfileSelector(overrides));
        return buildContext(
                this,
                overrides,
                repositorySystem,
                settingsBuilder,
                settingsDecrypter,
                profileSelector,
                repositorySystem::shutdown);
    }

    protected RepositorySystem createRepositorySystem(ContextOverrides contextOverrides) {
        return new RepositorySystemSupplier().get();
    }

    protected SettingsBuilder createSettingsBuilder(ContextOverrides contextOverrides) {
        return new SettingsBuilderSupplier().get();
    }

    protected SettingsDecrypter createSettingsDecrypter(ContextOverrides contextOverrides) {
        return new SettingsDecrypterSupplier(contextOverrides).get();
    }

    protected ProfileSelector createProfileSelector(ContextOverrides contextOverrides) {
        return new ProfileSelectorSupplier().get();
    }
}
