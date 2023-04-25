package eu.maveniverse.maven.mima.runtime.standalonestatic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.runtime.shared.StandaloneRuntimeSupport;
import java.util.function.Supplier;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;

public final class StandaloneStaticRuntime extends StandaloneRuntimeSupport {

    private final Supplier<RepositorySystem> repositorySystemSupplier;

    private final Supplier<SettingsBuilder> settingsBuilderSupplier;

    private final Supplier<SettingsDecrypter> settingsDecrypterSupplier;

    public StandaloneStaticRuntime() {
        this(
                "standalone-static",
                40,
                new RepositorySystemSupplier(),
                new SettingsBuilderSupplier(),
                new SettingsDecrypterSupplier());
    }

    public StandaloneStaticRuntime(
            String name,
            int priority,
            Supplier<RepositorySystem> repositorySystemSupplier,
            Supplier<SettingsBuilder> settingsBuilderSupplier,
            Supplier<SettingsDecrypter> settingsDecrypterSupplier) {
        super(name, priority);
        this.repositorySystemSupplier = requireNonNull(repositorySystemSupplier);
        this.settingsBuilderSupplier = requireNonNull(settingsBuilderSupplier);
        this.settingsDecrypterSupplier = requireNonNull(settingsDecrypterSupplier);
    }

    @Override
    public boolean managedRepositorySystem() {
        return true;
    }

    @Override
    public Context create(ContextOverrides overrides) {
        RepositorySystem repositorySystem = requireNonNull(repositorySystemSupplier.get());
        SettingsBuilder settingsBuilder = requireNonNull(settingsBuilderSupplier.get());
        SettingsDecrypter settingsDecrypter = requireNonNull(settingsDecrypterSupplier.get());
        return buildContext(
                this, overrides, repositorySystem, settingsBuilder, settingsDecrypter, repositorySystem::shutdown);
    }
}
