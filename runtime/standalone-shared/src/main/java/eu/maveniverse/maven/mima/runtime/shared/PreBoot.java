package eu.maveniverse.maven.mima.runtime.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.MavenUserHome;
import eu.maveniverse.maven.mima.context.internal.MavenSystemHomeImpl;
import eu.maveniverse.maven.mima.context.internal.MavenUserHomeImpl;
import java.nio.file.Path;

/**
 * Pre-boot derived and pre-processed state of MIMA, with not all configuration applied (!), as settings.xml
 * was not yet processed (and it may alter {@link MavenUserHome#localRepository()}).
 * <p>
 * For internal use only in runtimes.
 */
public final class PreBoot {
    private final ContextOverrides overrides;

    private final MavenUserHomeImpl mavenUserHome;

    private final MavenSystemHomeImpl mavenSystemHome;

    private final Path baseDir;

    public PreBoot(
            ContextOverrides overrides,
            MavenUserHomeImpl mavenUserHome,
            MavenSystemHomeImpl mavenSystemHome,
            Path baseDir) {
        this.overrides = requireNonNull(overrides);
        this.mavenUserHome = requireNonNull(mavenUserHome);
        this.mavenSystemHome = mavenSystemHome; // nullable
        this.baseDir = requireNonNull(baseDir);
    }

    public ContextOverrides getOverrides() {
        return overrides;
    }

    public MavenUserHomeImpl getMavenUserHome() {
        return mavenUserHome;
    }

    public MavenSystemHomeImpl getMavenSystemHome() {
        return mavenSystemHome;
    }

    public Path getBaseDir() {
        return baseDir;
    }
}
