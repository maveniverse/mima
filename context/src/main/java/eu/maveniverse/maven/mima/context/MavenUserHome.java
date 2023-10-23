package eu.maveniverse.maven.mima.context;

import java.nio.file.Path;

/**
 * Interface pointing to Maven User Home and various locations of interests within it.
 *
 * @since TBD
 */
public interface MavenUserHome {
    Path basedir();

    Path settingsXml();

    Path settingsSecurityXml();

    Path toolchainsXml();

    Path localRepository();
}
