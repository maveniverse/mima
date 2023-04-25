package eu.maveniverse.maven.mima.runtime.standalonestatic;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import java.util.Collections;
import java.util.function.Supplier;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

/**
 * Override to customize.
 */
public class SettingsDecrypterSupplier implements Supplier<SettingsDecrypter> {
    @Override
    public SettingsDecrypter get() {
        DefaultPlexusCipher plexusCipher = new DefaultPlexusCipher();
        DefaultSecDispatcher secDispatcher = new DefaultSecDispatcher(
                plexusCipher, Collections.emptyMap(), ContextOverrides.USER_SETTINGS_SECURITY_XML.toString());
        return new DefaultSettingsDecrypter(secDispatcher);
    }
}
