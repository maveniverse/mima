package eu.maveniverse.maven.mima.runtime.standalonestatic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.runtime.shared.StandaloneRuntimeSupport;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;

public final class StandaloneStaticRuntime extends StandaloneRuntimeSupport {

    private final Factory factory;

    public StandaloneStaticRuntime() {
        this("standalone-static", 40, new RepositorySystemFactory());
    }

    public StandaloneStaticRuntime(String name, int priority, Factory factory) {
        super(name, priority);
        this.factory = requireNonNull(factory);
    }

    @Override
    public boolean managedRepositorySystem() {
        return true;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        RepositorySystem repositorySystem = requireNonNull(factory.repositorySystem());
        SettingsBuilder settingsBuilder = requireNonNull(factory.settingsBuilder());
        SettingsDecrypter settingsDecrypter = requireNonNull(factory.settingsDecrypter());
        return buildContext(
                this, overrides, repositorySystem, settingsBuilder, settingsDecrypter, repositorySystem::shutdown);
    }

    public interface Factory {
        RepositorySystem repositorySystem();

        SettingsBuilder settingsBuilder();

        SettingsDecrypter settingsDecrypter();
    }
}
