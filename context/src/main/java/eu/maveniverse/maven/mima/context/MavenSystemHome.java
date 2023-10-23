package eu.maveniverse.maven.mima.context;

import java.nio.file.Path;

/**
 * Interface pointing to Maven System Home and various locations of interests within it.
 *
 * @since 2.4.0
 */
public interface MavenSystemHome {
    Path basedir();

    Path bin();

    Path boot();

    Path conf();

    Path lib();

    Path libExt();

    Path settingsXml();

    Path toolchainsXml();
}
