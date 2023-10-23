package eu.maveniverse.maven.mima.runtime.standalonestatic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.runtime.shared.PreBoot;
import eu.maveniverse.maven.mima.runtime.shared.StandaloneRuntimeSupport;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

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
        PreBoot preBoot = preBoot(overrides);
        RepositorySystem repositorySystem = requireNonNull(createRepositorySystem(preBoot));
        SettingsBuilder settingsBuilder = requireNonNull(createSettingsBuilder(preBoot));
        SettingsDecrypter settingsDecrypter = requireNonNull(createSettingsDecrypter(preBoot));
        ProfileSelector profileSelector = requireNonNull(createProfileSelector(preBoot));
        return buildContext(
                this,
                preBoot,
                repositorySystem,
                settingsBuilder,
                settingsDecrypter,
                profileSelector,
                repositorySystem::shutdown);
    }

    protected RepositorySystem createRepositorySystem(PreBoot preBoot) {
        return new RepositorySystemSupplier().get();
    }

    protected SettingsBuilder createSettingsBuilder(PreBoot preBoot) {
        return new SettingsBuilderSupplier().get();
    }

    protected SettingsDecrypter createSettingsDecrypter(PreBoot preBoot) {
        return new SettingsDecrypterSupplier(preBoot.getMavenUserHome()).get();
    }

    protected ProfileSelector createProfileSelector(PreBoot preBoot) {
        return new ProfileSelectorSupplier().get();
    }
}
