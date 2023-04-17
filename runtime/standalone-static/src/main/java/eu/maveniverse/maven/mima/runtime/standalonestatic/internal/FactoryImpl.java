package eu.maveniverse.maven.mima.runtime.standalonestatic.internal;

import eu.maveniverse.maven.mima.runtime.standalonestatic.RepositorySystemFactory;
import eu.maveniverse.maven.mima.runtime.standalonestatic.StandaloneStaticRuntime;
import java.util.Collections;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.apache.maven.settings.validation.DefaultSettingsValidator;
import org.eclipse.aether.RepositorySystem;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

public class FactoryImpl implements StandaloneStaticRuntime.Factory {
    private final RepositorySystemFactory repositorySystemFactory = new RepositorySystemFactory();

    @Override
    public RepositorySystem repositorySystem() {
        return repositorySystemFactory.repositorySystem();
    }

    @Override
    public SettingsBuilder settingsBuilder() {
        return new DefaultSettingsBuilder(
                new DefaultSettingsReader(), new DefaultSettingsWriter(), new DefaultSettingsValidator());
    }

    @Override
    public SettingsDecrypter settingsDecrypter() {
        DefaultPlexusCipher plexusCipher = new DefaultPlexusCipher();
        DefaultSecDispatcher secDispatcher =
                new DefaultSecDispatcher(plexusCipher, Collections.emptyMap(), "~/.m2/settings-security.xml");
        return new DefaultSettingsDecrypter(secDispatcher);
    }
}
