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

    MavenUserHome derive(ContextOverrides contextOverrides);
}
