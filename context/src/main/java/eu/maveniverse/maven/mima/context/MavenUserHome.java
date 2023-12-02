package eu.maveniverse.maven.mima.context;

import java.nio.file.Path;

/**
 * Interface pointing to Maven User Home and various locations of interests within it.
 *
 * @since 2.4.0
 */
public interface MavenUserHome {
    Path basedir();

    Path settingsXml();

    Path settingsSecurityXml();

    Path toolchainsXml();

    Path localRepository();

    /**
     * Derives new maven user home from itself with overrides applied.
     *
     * @since 2.4.4
     */
    MavenUserHome derive(ContextOverrides contextOverrides);
}
