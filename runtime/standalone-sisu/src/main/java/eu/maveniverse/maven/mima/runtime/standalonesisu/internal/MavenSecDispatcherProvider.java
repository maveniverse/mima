package eu.maveniverse.maven.mima.runtime.standalonesisu.internal;

import eu.maveniverse.maven.mima.runtime.shared.PreBoot;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

@Singleton
@Named("maven")
public class MavenSecDispatcherProvider implements Provider<SecDispatcher> {
    private final SecDispatcher secDispatcher;

    @Inject
    public MavenSecDispatcherProvider(PreBoot preBoot, PlexusCipher plexusCipher, Map<String, PasswordDecryptor> pds) {
        this.secDispatcher = new DefaultSecDispatcher(
                plexusCipher,
                pds,
                preBoot.getMavenUserHome().settingsSecurityXml().toString());
    }

    @Override
    public SecDispatcher get() {
        return secDispatcher;
    }
}
