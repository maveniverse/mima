package eu.maveniverse.maven.mima.runtime.standalonestatic;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Lookup;
import eu.maveniverse.maven.mima.runtime.shared.PreBoot;
import java.util.Optional;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;

public class StaticLookup implements Lookup {
    private final ProfileSelector profileSelector;
    private final SettingsBuilder settingsBuilder;
    private final SettingsDecrypter settingsDecrypter;

    public StaticLookup(PreBoot preBoot) {
        requireNonNull(preBoot);
        this.profileSelector = new ProfileSelectorSupplier().get();
        this.settingsBuilder = new SettingsBuilderSupplier().get();
        this.settingsDecrypter = new SettingsDecrypterSupplier(preBoot.getMavenUserHome()).get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> lookup(Class<T> type) {
        if (type.isAssignableFrom(ProfileSelector.class)) {
            return (Optional<T>) Optional.of(profileSelector);
        } else if (type.isAssignableFrom(SettingsBuilder.class)) {
            return (Optional<T>) Optional.of(settingsBuilder);
        } else if (type.isAssignableFrom(SettingsDecrypter.class)) {
            return (Optional<T>) Optional.of(settingsDecrypter);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> lookup(Class<T> type, String name) {
        if ("".equals(name) || "default".equals(name)) {
            return lookup(type);
        }
        return Optional.empty();
    }
}
